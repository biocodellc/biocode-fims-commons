package biocode.fims.query;

import biocode.fims.digester.Attribute;
import biocode.fims.digester.Entity;
import biocode.fims.fimsExceptions.FimsRuntimeException;
import biocode.fims.fimsExceptions.errorCodes.QueryCode;
import biocode.fims.models.Project;
import biocode.fims.projectConfig.ProjectConfig;
import biocode.fims.query.dsl.*;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;

/**
 * @author rjewing
 */
public class QueryBuilderTest {

    @Test
    public void should_throw_exception_if_no_visits() {
        try {
            queryBuilder("event").parameterizedQuery(false);
            fail();
        } catch (Exception e) {
            if (e instanceof FimsRuntimeException) {
                assertEquals(QueryCode.INVALID_QUERY, ((FimsRuntimeException) e).getErrorCode());
            } else {
                fail();
            }
        }
    }

    @Test
    public void should_throw_exception_when_visiting_empty_expression_query() {
        try {
            queryBuilder("event").visit(new EmptyExpression());
            fail();
        } catch (Exception e) {
            if (e instanceof FimsRuntimeException) {
                assertEquals(QueryCode.INVALID_QUERY, ((FimsRuntimeException) e).getErrorCode());
            } else {
                fail();
            }
        }
    }

    @Test
    public void should_throw_exception_when_visiting_all_expression_with_other_expression_query() {
        try {
            QueryBuilder builder = queryBuilder("event");
            builder.visit(new AllExpression());
            builder.visit(new FTSExpression(null, "something"));
            builder.parameterizedQuery(false);
            fail();
        } catch (Exception e) {
            if (e instanceof FimsRuntimeException) {
                assertEquals(QueryCode.INVALID_QUERY, ((FimsRuntimeException) e).getErrorCode());
            } else {
                fail();
            }
        }
    }

    @Test
    public void should_throw_exception_when_missing_attribute_for_column() {
        try {
            queryBuilder("event").visit(new ComparisonExpression("non_existent_column", "test", ComparisonOperator.EQUALS));
            fail();
        } catch (Exception e) {
            if (e instanceof FimsRuntimeException) {
                assertEquals(QueryCode.UNKNOWN_COLUMN, ((FimsRuntimeException) e).getErrorCode());
            } else {
                fail();
            }
        }
    }

    @Test
    public void should_throw_exception_when_missing_attribute_for_column_path() {
        try {
            queryBuilder("event").visit(new ComparisonExpression("sample.col2", "test", ComparisonOperator.EQUALS));
            fail();
        } catch (Exception e) {
            if (e instanceof FimsRuntimeException) {
                assertEquals(QueryCode.UNKNOWN_COLUMN, ((FimsRuntimeException) e).getErrorCode());
            } else {
                fail();
            }
        }
    }

    @Test
    public void should_throw_exception_when_missing_entity_specified_in_column_path() {
        try {
            queryBuilder("event").visit(new ComparisonExpression("missing_entity.col4", "test", ComparisonOperator.EQUALS));
            fail();
        } catch (Exception e) {
            if (e instanceof FimsRuntimeException) {
                assertEquals(QueryCode.UNKNOWN_ENTITY, ((FimsRuntimeException) e).getErrorCode());
            } else {
                fail();
            }
        }
    }

    @Test
    public void should_throw_exception_when_invalid_entity_specified_in_column_path_for_single_entity_project() {
        try {
            singleEntityProjectQueryBuilder().visit(new ComparisonExpression("sample.eventId", "test", ComparisonOperator.EQUALS));
            fail();
        } catch (Exception e) {
            if (e instanceof FimsRuntimeException) {
                assertEquals(QueryCode.UNKNOWN_COLUMN, ((FimsRuntimeException) e).getErrorCode());
            } else {
                fail();
            }
        }
    }

    @Test
    public void should_throw_exception_for_multi_entity_query_with_no_relation_to_query_entity() {
        try {
            QueryBuilder queryBuilder = queryBuilder("non-linked");
            queryBuilder.visit(new ComparisonExpression("sample.eventId", "test", ComparisonOperator.EQUALS));
            queryBuilder.parameterizedQuery(false);
            fail();
        } catch (Exception e) {
            if (e instanceof FimsRuntimeException) {
                assertEquals(QueryCode.UNRELATED_ENTITIES, ((FimsRuntimeException) e).getErrorCode());
            } else {
                fail();
            }
        }
    }

    @Test
    public void should_write_valid_sql_for_single_entity_project() {
        QueryBuilder queryBuilder = singleEntityProjectQueryBuilder();
        queryBuilder.visit(new ComparisonExpression("col2", "value", ComparisonOperator.LESS_THEN_EQUAL));

        Map<String, String> params = new HashMap<>();
        params.put("1", "value");
        ParametrizedQuery expected = new ParametrizedQuery(
                "SELECT data FROM project_1.event AS event WHERE event.data->>'urn:col2' <= :1",
                params
        );
        assertEquals(expected, queryBuilder.parameterizedQuery(false));
    }

    @Test
    public void should_write_valid_sql_for_single_entity_project_with_column_path() {
        QueryBuilder queryBuilder = singleEntityProjectQueryBuilder();
        queryBuilder.visit(new ComparisonExpression("event.col2", "value", ComparisonOperator.LESS_THEN_EQUAL));

        Map<String, String> params = new HashMap<>();
        params.put("1", "value");
        ParametrizedQuery expected = new ParametrizedQuery(
                "SELECT data FROM project_1.event AS event WHERE event.data->>'urn:col2' <= :1",
                params
        );
        assertEquals(expected, queryBuilder.parameterizedQuery(false));
    }

    @Test
    public void should_default_to_given_entity_attribute_uri_for_ambiguous_column() {
        QueryBuilder queryBuilder = queryBuilder("sample");
        queryBuilder.visit(new ComparisonExpression("eventId", "1", ComparisonOperator.EQUALS));

        Map<String, String> params = new HashMap<>();
        params.put("1", "1");
        ParametrizedQuery expected = new ParametrizedQuery(
                "SELECT data FROM project_1.sample AS sample WHERE sample.data->>'sample_eventId' = :1",
                params
        );
        assertEquals(expected, queryBuilder.parameterizedQuery(false));
    }

    @Test
    public void should_parse_dot_path_column() {
        QueryBuilder queryBuilder = queryBuilder("event");
        queryBuilder.visit(new ComparisonExpression("sample.eventId", "1", ComparisonOperator.EQUALS));

        Map<String, String> params = new HashMap<>();
        params.put("1", "1");
        ParametrizedQuery expected = new ParametrizedQuery(
                "SELECT data FROM project_1.event AS event JOIN project_1.sample AS sample " +
                        "ON sample.data->>'sample_eventId' = event.local_identifier and sample.expedition_id = event.expedition_id " +
                        "WHERE sample.data->>'sample_eventId' = :1",
                params
        );
        assertEquals(expected, queryBuilder.parameterizedQuery(false));
    }

    @Test
    public void should_include_only_public_expeditions_statement() {
        QueryBuilder queryBuilder = queryBuilder("event");
        queryBuilder.visit(new AllExpression());

        ParametrizedQuery expected = new ParametrizedQuery(
                "SELECT data FROM project_1.event AS event JOIN expeditions ON expeditions.id = event.expedition_id " +
                        "WHERE expeditions.public = true",
                new HashMap<>()
        );
        assertEquals(expected, queryBuilder.parameterizedQuery(true));
    }

    @Test
    public void should_include_only_public_expeditions_as_outside_AND_expression_if_another_expression_present() {
        QueryBuilder queryBuilder = queryBuilder("event");
        queryBuilder.visit(new ComparisonExpression("col2", "value", ComparisonOperator.EQUALS));

        Map<String, String> params = new HashMap<>();
        params.put("1", "value");
        ParametrizedQuery expected = new ParametrizedQuery(
                "SELECT data FROM project_1.event AS event JOIN expeditions ON expeditions.id = event.expedition_id " +
                        "WHERE (event.data->>'urn:col2' = :1) AND expeditions.public = true",
                params
        );
        assertEquals(expected, queryBuilder.parameterizedQuery(true));
    }

    @Test
    public void should_write_valid_sql_for_comparison_expression() {
        QueryBuilder queryBuilder = queryBuilder("event");
        queryBuilder.visit(new ComparisonExpression("col2", "value", ComparisonOperator.EQUALS));

        Map<String, String> params = new HashMap<>();
        params.put("1", "value");
        ParametrizedQuery expected = new ParametrizedQuery(
                "SELECT data FROM project_1.event AS event WHERE event.data->>'urn:col2' = :1",
                params
        );
        assertEquals(expected, queryBuilder.parameterizedQuery(false));
    }

    @Test
    public void should_write_valid_sql_for_exists_expression_single_column() {
        QueryBuilder queryBuilder = queryBuilder("event");
        queryBuilder.visit(new ExistsExpression("col2"));

        Map<String, String> params = new HashMap<>();
        params.put("1", "urn:col2");
        ParametrizedQuery expected = new ParametrizedQuery(
                "SELECT data FROM project_1.event AS event WHERE event.data ?? :1",
                params
        );
        assertEquals(expected, queryBuilder.parameterizedQuery(false));
    }

    @Test
    public void should_write_valid_sql_for_exists_expression_multiple_columns_same_entity() {
        QueryBuilder queryBuilder = queryBuilder("event");
        queryBuilder.visit(new ExistsExpression("[col2, col3]"));

        Map<String, String> params = new HashMap<>();
        params.put("1", "urn:col2");
        params.put("2", "urn:event_col3");
        ParametrizedQuery expected = new ParametrizedQuery(
                "SELECT data FROM project_1.event AS event WHERE event.data ??& array[:1, :2]",
                params
        );
        assertEquals(expected, queryBuilder.parameterizedQuery(false));
    }

    @Test
    public void should_write_valid_sql_for_exists_expression_multiple_columns_multiple_entity() {
        QueryBuilder queryBuilder = queryBuilder("event");
        queryBuilder.visit(new ExistsExpression("[col2, col3, sample.col3]"));

        Map<String, String> params = new HashMap<>();
        params.put("1", "urn:col2");
        params.put("2", "urn:event_col3");
        params.put("3", "urn:col3");
        ParametrizedQuery expected = new ParametrizedQuery(
                "SELECT data FROM project_1.event AS event " +
                        "JOIN project_1.sample AS sample ON sample.data->>'sample_eventId' = event.local_identifier and sample.expedition_id = event.expedition_id " +
                        "WHERE (event.data ??& array[:1, :2] AND sample.data ?? :3)",
                params
        );
        assertEquals(expected, queryBuilder.parameterizedQuery(false));
    }

    @Test
    public void should_write_valid_sql_for_expedition_expression_single_expedition() {
        QueryBuilder queryBuilder = queryBuilder("event");
        queryBuilder.visit(new ExpeditionExpression("TEST"));

        Map<String, String> params = new HashMap<>();
        params.put("1", "TEST");
        ParametrizedQuery expected = new ParametrizedQuery(
                "SELECT data FROM project_1.event AS event " +
                        "JOIN expeditions ON expeditions.id = event.expedition_id " +
                        "WHERE expeditions.expedition_code = :1",
                params
        );
        assertEquals(expected, queryBuilder.parameterizedQuery(false));
    }

    @Test
    public void should_write_valid_sql_for_expedition_expression_multiple_expedition() {
        QueryBuilder queryBuilder = queryBuilder("event");
        queryBuilder.visit(new ExpeditionExpression("TEST, TEST2"));

        Map<String, String> params = new HashMap<>();
        params.put("1", "TEST");
        params.put("2", "TEST2");
        ParametrizedQuery expected = new ParametrizedQuery(
                "SELECT data FROM project_1.event AS event " +
                        "JOIN expeditions ON expeditions.id = event.expedition_id " +
                        "WHERE expeditions.expedition_code IN (:1, :2)",
                params
        );
        assertEquals(expected, queryBuilder.parameterizedQuery(false));
    }

    @Test
    public void should_write_valid_sql_for_fts_expression_no_column() {
        QueryBuilder queryBuilder = queryBuilder("event");
        queryBuilder.visit(new FTSExpression(null, "alligators"));

        Map<String, String> params = new HashMap<>();
        params.put("1", "alligators");
        ParametrizedQuery expected = new ParametrizedQuery(
                "SELECT data FROM project_1.event AS event " +
                        "WHERE event.tsv @@ to_tsquery(:1)",
                params
        );
        assertEquals(expected, queryBuilder.parameterizedQuery(false));
    }

    @Test
    public void should_write_valid_sql_for_fts_expression_with_column() {
        QueryBuilder queryBuilder = queryBuilder("event");
        queryBuilder.visit(new FTSExpression("col2", "alligators"));

        Map<String, String> params = new HashMap<>();
        params.put("1", "alligators");
        ParametrizedQuery expected = new ParametrizedQuery(
                "SELECT data FROM project_1.event AS event " +
                        "WHERE (to_tsvector(event.data->>'urn:col2') @@ to_tsquery(:1) AND event.tsv @@ to_tsquery(:1))",
                params
        );
        assertEquals(expected, queryBuilder.parameterizedQuery(false));
    }

    @Test
    public void should_write_valid_sql_for_like_expression() {
        QueryBuilder queryBuilder = queryBuilder("event");
        queryBuilder.visit(new LikeExpression("col2", "%val%2"));

        Map<String, String> params = new HashMap<>();
        params.put("1", "%val%2");
        ParametrizedQuery expected = new ParametrizedQuery(
                "SELECT data FROM project_1.event AS event WHERE event.data->>'urn:col2' ILIKE :1",
                params
        );
        assertEquals(expected, queryBuilder.parameterizedQuery(false));
    }

    @Test
    public void should_write_valid_sql_for_and_expression() {
        QueryBuilder queryBuilder = queryBuilder("event");

        LogicalExpression andExp = new LogicalExpression(
                LogicalOperator.AND,
                new ComparisonExpression("col2", "1", ComparisonOperator.LESS_THEN_EQUAL),
                new ComparisonExpression("col3", "test", ComparisonOperator.EQUALS)
        );

        queryBuilder.visit(andExp);

        Map<String, String> params = new HashMap<>();
        params.put("1", "1");
        params.put("2", "test");
        ParametrizedQuery expected = new ParametrizedQuery(
                "SELECT data FROM project_1.event AS event " +
                        "WHERE event.data->>'urn:col2' <= :1 AND event.data->>'urn:event_col3' = :2",
                params
        );
        assertEquals(expected, queryBuilder.parameterizedQuery(false));
    }

    @Test
    public void should_write_valid_sql_for_nested_logical_expression() {
        QueryBuilder queryBuilder = queryBuilder("event");

        LogicalExpression andExp = new LogicalExpression(
                LogicalOperator.AND,
                new ComparisonExpression("col2", "1", ComparisonOperator.LESS_THEN_EQUAL),
                new ComparisonExpression("col3", "test", ComparisonOperator.EQUALS)
        );

        LogicalExpression root = new LogicalExpression(
                LogicalOperator.OR,
                andExp,
                new ExistsExpression("col2")
        );

        queryBuilder.visit(root);

        String expectedSql = "SELECT data FROM project_1.event AS event " +
                "WHERE " +
                "event.data->>'urn:col2' <= :1 AND event.data->>'urn:event_col3' = :2 " +
                "OR event.data ?? :3";

        Map<String, String> params = new HashMap<>();
        params.put("1", "1");
        params.put("2", "test");
        params.put("3", "urn:col2");
        ParametrizedQuery expected = new ParametrizedQuery(expectedSql, params);
        assertEquals(expected, queryBuilder.parameterizedQuery(false));
    }

    @Test
    public void should_write_valid_sql_for_range_expression_inclusive() {
        QueryBuilder queryBuilder = queryBuilder("event");
        queryBuilder.visit(new RangeExpression("col2", "[1 TO 10]"));

        Map<String, String> params = new HashMap<>();
        params.put("1", "1");
        params.put("2", "10");
        ParametrizedQuery expected = new ParametrizedQuery(
                "SELECT data FROM project_1.event AS event WHERE (event.data->>'urn:col2' >= :1 AND event.data->>'urn:col2' <= :2)",
                params
        );
        assertEquals(expected, queryBuilder.parameterizedQuery(false));
    }

    @Test
    public void should_write_valid_sql_for_range_expression_exclusive() {
        QueryBuilder queryBuilder = queryBuilder("event");
        queryBuilder.visit(new RangeExpression("col2", "{1 TO 10}"));

        Map<String, String> params = new HashMap<>();
        params.put("1", "1");
        params.put("2", "10");
        ParametrizedQuery expected = new ParametrizedQuery(
                "SELECT data FROM project_1.event AS event WHERE (event.data->>'urn:col2' > :1 AND event.data->>'urn:col2' < :2)",
                params
        );
        assertEquals(expected, queryBuilder.parameterizedQuery(false));
    }

    @Test
    public void should_write_valid_sql_for_range_expression_mixed_inclusive_exclusive() {
        QueryBuilder queryBuilder = queryBuilder("event");
        queryBuilder.visit(new RangeExpression("col2", "{1 TO 10]"));

        Map<String, String> params = new HashMap<>();
        params.put("1", "1");
        params.put("2", "10");
        ParametrizedQuery expected = new ParametrizedQuery(
                "SELECT data FROM project_1.event AS event WHERE (event.data->>'urn:col2' > :1 AND event.data->>'urn:col2' <= :2)",
                params
        );
        assertEquals(expected, queryBuilder.parameterizedQuery(false));
    }

    @Test
    public void should_write_valid_sql_for_range_expression_one_sided_range() {
        QueryBuilder queryBuilder = queryBuilder("event");
        queryBuilder.visit(new RangeExpression("col2", "{* TO 10]"));

        Map<String, String> params = new HashMap<>();
        params.put("1", "10");
        ParametrizedQuery expected = new ParametrizedQuery(
                "SELECT data FROM project_1.event AS event WHERE (event.data->>'urn:col2' <= :1)",
                params
        );
        assertEquals(expected, queryBuilder.parameterizedQuery(false));
    }

    @Test
    public void should_write_valid_sql_for_all_expression() {
        QueryBuilder queryBuilder = queryBuilder("event");
        queryBuilder.visit(new AllExpression());

        String expectedSql = "SELECT data FROM project_1.event AS event";

        Map<String, String> params = new HashMap<>();
        ParametrizedQuery expected = new ParametrizedQuery(expectedSql, params);
        assertEquals(expected, queryBuilder.parameterizedQuery(false));
    }

    @Test
    public void should_write_valid_sql_for_group_expression() {
        QueryBuilder queryBuilder = queryBuilder("event");

        GroupExpression g1 = new GroupExpression(
                new LogicalExpression(
                        LogicalOperator.OR,
                        new ComparisonExpression("col2", "1", ComparisonOperator.LESS_THEN_EQUAL),
                        new ComparisonExpression("col2", "4", ComparisonOperator.EQUALS)
                )
        );
        LogicalExpression andExpRight = new LogicalExpression(
                LogicalOperator.AND,
                g1,
                new ExistsExpression("col3")
        );
        LogicalExpression root = new LogicalExpression(
                LogicalOperator.AND,
                new ComparisonExpression("eventId", "100", ComparisonOperator.LESS_THEN_EQUAL),
                andExpRight
        );

        queryBuilder.visit(root);

        String expectedSql = "SELECT data FROM project_1.event AS event " +
                "WHERE " +
                "event.data->>'eventId' <= :1 AND " +
                "(event.data->>'urn:col2' <= :2 OR event.data->>'urn:col2' = :3) " +
                "AND event.data ?? :4";

        Map<String, String> params = new HashMap<>();
        params.put("1", "100");
        params.put("2", "1");
        params.put("3", "4");
        params.put("4", "urn:event_col3");
        ParametrizedQuery expected = new ParametrizedQuery(expectedSql, params);
        assertEquals(expected, queryBuilder.parameterizedQuery(false));
    }

    @Test
    public void should_add_joins_for_multi_entity_query_with_direct_relationship() {
        QueryBuilder queryBuilder = queryBuilder("event");
        queryBuilder.visit(new ComparisonExpression("sampleId", "value", ComparisonOperator.EQUALS));

        String expectedSql = "SELECT data FROM project_1.event AS event " +
                "JOIN project_1.sample AS sample ON sample.data->>'sample_eventId' = event.local_identifier and sample.expedition_id = event.expedition_id " +
                "WHERE sample.data->>'urn:sampleId' = :1";

        Map<String, String> params = new HashMap<>();
        params.put("1", "value");
        ParametrizedQuery expected = new ParametrizedQuery(expectedSql, params);
        assertEquals(expected, queryBuilder.parameterizedQuery(false));
    }

    @Test
    public void should_add_joins_for_multi_entity_query_with_non_direct_relationship_child_entity_query() {
        QueryBuilder queryBuilder = queryBuilder("tissue");
        queryBuilder.visit(new ComparisonExpression("event.eventId", "1", ComparisonOperator.EQUALS));

        String expectedSql = "SELECT data FROM project_1.tissue AS tissue " +
                "JOIN project_1.sample AS sample ON sample.local_identifier = tissue.data->>'tissue_sampleId' and sample.expedition_id = tissue.expedition_id " +
                "JOIN project_1.event AS event ON event.local_identifier = sample.data->>'sample_eventId' and event.expedition_id = sample.expedition_id " +
                "WHERE event.data->>'eventId' = :1";

        Map<String, String> params = new HashMap<>();
        params.put("1", "1");
        ParametrizedQuery expected = new ParametrizedQuery(expectedSql, params);
        assertEquals(expected, queryBuilder.parameterizedQuery(false));
    }

    @Test
    public void should_add_joins_for_multi_entity_query_with_non_direct_relationship_with_parent_entity_query() {
        QueryBuilder queryBuilder = queryBuilder("event");
        queryBuilder.visit(new ComparisonExpression("tissue.tissueId", "1", ComparisonOperator.EQUALS));

        String expectedSql = "SELECT data FROM project_1.event AS event " +
                "JOIN project_1.sample AS sample ON sample.data->>'sample_eventId' = event.local_identifier and sample.expedition_id = event.expedition_id " +
                "JOIN project_1.tissue AS tissue ON tissue.data->>'tissue_sampleId' = sample.local_identifier and tissue.expedition_id = sample.expedition_id " +
                "WHERE tissue.data->>'urn:tissueId' = :1";

        Map<String, String> params = new HashMap<>();
        params.put("1", "1");
        ParametrizedQuery expected = new ParametrizedQuery(expectedSql, params);
        assertEquals(expected, queryBuilder.parameterizedQuery(false));
    }

    @Test
    public void should_add_joins_for_multi_entity_query_with_multiple_direct_relationship() {
        QueryBuilder queryBuilder = queryBuilder("event");

        LogicalExpression andExp = new LogicalExpression(
                LogicalOperator.AND,
                new ComparisonExpression("tissue.tissueId", "1", ComparisonOperator.EQUALS),
                new ComparisonExpression("sample.sampleId", "1", ComparisonOperator.EQUALS)
        );

        queryBuilder.visit(andExp);

        String expectedSql = "SELECT data FROM project_1.event AS event " +
                "JOIN project_1.sample AS sample ON sample.data->>'sample_eventId' = event.local_identifier and sample.expedition_id = event.expedition_id " +
                "JOIN project_1.tissue AS tissue ON tissue.data->>'tissue_sampleId' = sample.local_identifier and tissue.expedition_id = sample.expedition_id " +
                "WHERE tissue.data->>'urn:tissueId' = :1 AND sample.data->>'urn:sampleId' = :2";

        Map<String, String> params = new HashMap<>();
        params.put("1", "1");
        params.put("2", "1");
        ParametrizedQuery expected = new ParametrizedQuery(expectedSql, params);
        assertEquals(expected, queryBuilder.parameterizedQuery(false));
    }


    private QueryBuilder queryBuilder(String conceptAlias) {
        return new QueryBuilder(project(), conceptAlias);
    }

    private QueryBuilder singleEntityProjectQueryBuilder() {
        ProjectConfig config = new ProjectConfig();
        config.addEntity(event());

        Project project = project();
        project.setProjectConfig(config);

        return new QueryBuilder(project, "event");
    }


    private Project project() {
        ProjectConfig config = new ProjectConfig();
        config.addEntity(event());
        config.addEntity(sample());
        config.addEntity(tissue());
        config.addEntity(non_linked_entity());

        Project project = new Project.ProjectBuilder("TEST", null, config, null)
                .build();

        project.setProjectId(1);

        return project;
    }

    private Entity event() {
        Entity e = new Entity("event", "someURI");
        e.setUniqueKey("eventId");
        e.addAttribute(new Attribute("eventId", "eventId"));
        e.addAttribute(new Attribute("col2", "urn:col2"));
        e.addAttribute(new Attribute("col3", "urn:event_col3"));
        return e;
    }

    private Entity sample() {
        Entity e = new Entity("sample", "someURI");
        e.setParentEntity("event");
        e.setUniqueKey("sampleId");
        e.addAttribute(new Attribute("sampleId", "urn:sampleId"));
        e.addAttribute(new Attribute("eventId", "sample_eventId"));
        e.addAttribute(new Attribute("col3", "urn:col3"));
        e.addAttribute(new Attribute("col4", "urn:col4"));
        return e;
    }

    private Entity tissue() {
        Entity e = new Entity("tissue", "someURI");
        e.setParentEntity("sample");
        e.setUniqueKey("tissueId");
        e.addAttribute(new Attribute("tissueId", "urn:tissueId"));
        e.addAttribute(new Attribute("sampleId", "tissue_sampleId"));
        e.addAttribute(new Attribute("col3", "urn:col3"));
        e.addAttribute(new Attribute("col4", "urn:col4"));
        return e;
    }

    private Entity non_linked_entity() {
        Entity e = new Entity("non-linked", "someURI");
        e.setUniqueKey("id");
        e.addAttribute(new Attribute("id", "urn:id"));
        e.addAttribute(new Attribute("col3", "urn:col3"));
        e.addAttribute(new Attribute("col4", "urn:col4"));
        return e;
    }

}