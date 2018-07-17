package biocode.fims.query;

import biocode.fims.projectConfig.models.Attribute;
import biocode.fims.projectConfig.models.DataType;
import biocode.fims.projectConfig.models.Entity;
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
    public void should_throw_exception_for_multi_entity_select_with_no_relation_to_query_entity() {
        try {
            QueryBuilder queryBuilder = queryBuilder("non-linked");
            queryBuilder.visit(new SelectExpression("sample", new AllExpression()));
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
    public void should_include_casts_for_non_string_comparisons() {
        // int
        QueryBuilder queryBuilder = singleEntityProjectQueryBuilder();
        queryBuilder.visit(new ComparisonExpression("intCol", "1", ComparisonOperator.LESS_THEN_EQUAL));

        Map<String, String> params = new HashMap<>();
        params.put("1", "1");
        ParametrizedQuery expected = new ParametrizedQuery(
                "SELECT event.data AS \"event_data\", event_entity_identifiers.identifier AS \"event_root_identifier\" FROM project_1.event AS event LEFT JOIN entity_identifiers AS event_entity_identifiers ON event_entity_identifiers.expedition_id = event.expedition_id and event_entity_identifiers.concept_alias = 'event' WHERE convert_to_int(event.data->>'urn:intCol') <= :1::int",
                params
        );
        assertEquals(expected, queryBuilder.parameterizedQuery(false));

        // float
        queryBuilder = singleEntityProjectQueryBuilder();
        queryBuilder.visit(new ComparisonExpression("floatCol", "1", ComparisonOperator.LESS_THEN));

        params.clear();
        params.put("1", "1");
        expected = new ParametrizedQuery(
                "SELECT event.data AS \"event_data\", event_entity_identifiers.identifier AS \"event_root_identifier\" FROM project_1.event AS event LEFT JOIN entity_identifiers AS event_entity_identifiers ON event_entity_identifiers.expedition_id = event.expedition_id and event_entity_identifiers.concept_alias = 'event' WHERE convert_to_float(event.data->>'urn:floatCol') < :1::float",
                params
        );
        assertEquals(expected, queryBuilder.parameterizedQuery(false));

        // date
        queryBuilder = singleEntityProjectQueryBuilder();
        queryBuilder.visit(new ComparisonExpression("dateCol", "2017-03-01", ComparisonOperator.GREATER_THEN_EQUAL));

        params.clear();
        params.put("1", "2017-03-01");
        expected = new ParametrizedQuery(
                "SELECT event.data AS \"event_data\", event_entity_identifiers.identifier AS \"event_root_identifier\" FROM project_1.event AS event LEFT JOIN entity_identifiers AS event_entity_identifiers ON event_entity_identifiers.expedition_id = event.expedition_id and event_entity_identifiers.concept_alias = 'event' WHERE convert_to_date(event.data->>'urn:dateCol') >= :1::date",
                params
        );
        assertEquals(expected, queryBuilder.parameterizedQuery(false));

        // datetime
        queryBuilder = singleEntityProjectQueryBuilder();
        queryBuilder.visit(new ComparisonExpression("datetimeCol", "2017-03-01T11:52:44.110", ComparisonOperator.GREATER_THEN));

        params.clear();
        params.put("1", "2017-03-01T11:52:44.110");
        expected = new ParametrizedQuery(
                "SELECT event.data AS \"event_data\", event_entity_identifiers.identifier AS \"event_root_identifier\" FROM project_1.event AS event LEFT JOIN entity_identifiers AS event_entity_identifiers ON event_entity_identifiers.expedition_id = event.expedition_id and event_entity_identifiers.concept_alias = 'event' WHERE convert_to_datetime(event.data->>'urn:datetimeCol') > :1::timestamp",
                params
        );
        assertEquals(expected, queryBuilder.parameterizedQuery(false));

        // time
        queryBuilder = singleEntityProjectQueryBuilder();
        queryBuilder.visit(new ComparisonExpression("timeCol", "11:52:44.110", ComparisonOperator.LESS_THEN));

        params.clear();
        params.put("1", "11:52:44.110");
        expected = new ParametrizedQuery(
                "SELECT event.data AS \"event_data\", event_entity_identifiers.identifier AS \"event_root_identifier\" FROM project_1.event AS event LEFT JOIN entity_identifiers AS event_entity_identifiers ON event_entity_identifiers.expedition_id = event.expedition_id and event_entity_identifiers.concept_alias = 'event' WHERE convert_to_time(event.data->>'urn:timeCol') < :1::time",
                params
        );
        assertEquals(expected, queryBuilder.parameterizedQuery(false));
    }

    @Test
    public void should_not_include_casts_for_string_comparisons() {
        QueryBuilder queryBuilder = singleEntityProjectQueryBuilder();
        queryBuilder.visit(new ComparisonExpression("col2", "val", ComparisonOperator.LESS_THEN_EQUAL));

        Map<String, String> params = new HashMap<>();
        params.put("1", "val");
        ParametrizedQuery expected = new ParametrizedQuery(
                "SELECT event.data AS \"event_data\", event_entity_identifiers.identifier AS \"event_root_identifier\" FROM project_1.event AS event LEFT JOIN entity_identifiers AS event_entity_identifiers ON event_entity_identifiers.expedition_id = event.expedition_id and event_entity_identifiers.concept_alias = 'event' WHERE event.data->>'urn:col2' <= :1",
                params
        );
        assertEquals(expected, queryBuilder.parameterizedQuery(false));
    }

    @Test
    public void should_not_include_casts_for_non_string_equals_comparisons() {
        QueryBuilder queryBuilder = singleEntityProjectQueryBuilder();
        queryBuilder.visit(new ComparisonExpression("intCol", "1", ComparisonOperator.EQUALS));

        Map<String, String> params = new HashMap<>();
        params.put("1", "1");
        ParametrizedQuery expected = new ParametrizedQuery(
                "SELECT event.data AS \"event_data\", event_entity_identifiers.identifier AS \"event_root_identifier\" FROM project_1.event AS event LEFT JOIN entity_identifiers AS event_entity_identifiers ON event_entity_identifiers.expedition_id = event.expedition_id and event_entity_identifiers.concept_alias = 'event' WHERE event.data->>'urn:intCol' = :1",
                params
        );
        assertEquals(expected, queryBuilder.parameterizedQuery(false));
    }

    @Test
    public void should_write_valid_sql_for_single_entity_project() {
        QueryBuilder queryBuilder = singleEntityProjectQueryBuilder();
        queryBuilder.visit(new ComparisonExpression("col2", "value", ComparisonOperator.LESS_THEN_EQUAL));

        Map<String, String> params = new HashMap<>();
        params.put("1", "value");
        ParametrizedQuery expected = new ParametrizedQuery(
                "SELECT event.data AS \"event_data\", event_entity_identifiers.identifier AS \"event_root_identifier\" FROM project_1.event AS event LEFT JOIN entity_identifiers AS event_entity_identifiers ON event_entity_identifiers.expedition_id = event.expedition_id and event_entity_identifiers.concept_alias = 'event' WHERE event.data->>'urn:col2' <= :1",
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
                "SELECT event.data AS \"event_data\", event_entity_identifiers.identifier AS \"event_root_identifier\" FROM project_1.event AS event LEFT JOIN entity_identifiers AS event_entity_identifiers ON event_entity_identifiers.expedition_id = event.expedition_id and event_entity_identifiers.concept_alias = 'event' WHERE event.data->>'urn:col2' <= :1",
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
                "SELECT sample.data AS \"sample_data\", sample_entity_identifiers.identifier AS \"sample_root_identifier\" FROM project_1.sample AS sample LEFT JOIN entity_identifiers AS sample_entity_identifiers ON sample_entity_identifiers.expedition_id = sample.expedition_id and sample_entity_identifiers.concept_alias = 'sample' WHERE sample.parent_identifier = :1",
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
                "SELECT event.data AS \"event_data\", event_entity_identifiers.identifier AS \"event_root_identifier\" FROM project_1.event AS event JOIN project_1.sample AS sample " +
                        "ON sample.parent_identifier = event.local_identifier and sample.expedition_id = event.expedition_id " +
                        "LEFT JOIN entity_identifiers AS event_entity_identifiers ON event_entity_identifiers.expedition_id = event.expedition_id and event_entity_identifiers.concept_alias = 'event' " +
                        "WHERE sample.parent_identifier = :1",
                params
        );
        assertEquals(expected, queryBuilder.parameterizedQuery(false));
    }

    @Test
    public void should_include_only_public_expeditions_statement() {
        QueryBuilder queryBuilder = queryBuilder("event");
        queryBuilder.visit(new AllExpression());

        ParametrizedQuery expected = new ParametrizedQuery(
                "SELECT event.data AS \"event_data\", event_entity_identifiers.identifier AS \"event_root_identifier\" FROM project_1.event AS event JOIN expeditions ON expeditions.id = event.expedition_id " +
                        "LEFT JOIN entity_identifiers AS event_entity_identifiers ON event_entity_identifiers.expedition_id = event.expedition_id and event_entity_identifiers.concept_alias = 'event' " +
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
                "SELECT event.data AS \"event_data\", event_entity_identifiers.identifier AS \"event_root_identifier\" FROM project_1.event AS event JOIN expeditions ON expeditions.id = event.expedition_id " +
                        "LEFT JOIN entity_identifiers AS event_entity_identifiers ON event_entity_identifiers.expedition_id = event.expedition_id and event_entity_identifiers.concept_alias = 'event' " +
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
                "SELECT event.data AS \"event_data\", event_entity_identifiers.identifier AS \"event_root_identifier\" FROM project_1.event AS event " +
                        "LEFT JOIN entity_identifiers AS event_entity_identifiers ON event_entity_identifiers.expedition_id = event.expedition_id and event_entity_identifiers.concept_alias = 'event' " +
                        "WHERE event.data->>'urn:col2' = :1",
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
                "SELECT event.data AS \"event_data\", event_entity_identifiers.identifier AS \"event_root_identifier\" FROM project_1.event AS event " +
                        "LEFT JOIN entity_identifiers AS event_entity_identifiers ON event_entity_identifiers.expedition_id = event.expedition_id and event_entity_identifiers.concept_alias = 'event' " +
                        "WHERE event.data ?? :1",
                params
        );
        assertEquals(expected, queryBuilder.parameterizedQuery(false));
    }

//    @Test
//    public void should_exclude_exists_expression_if_local_identifier() {
//        QueryBuilder queryBuilder = queryBuilder("event");
//        queryBuilder.visit(new ExistsExpression("eventId"));
//
//        Map<String, String> params = new HashMap<>();
//        params.put("1", "urn:col2");
//        ParametrizedQuery expected = new ParametrizedQuery(
//                "SELECT event.data AS \"event_data\", event_entity_identifiers.identifier AS \"event_root_identifier\" FROM project_1.event AS event " +
//                        "LEFT JOIN entity_identifiers AS event_entity_identifiers ON event_entity_identifiers.expedition_id = event.expedition_id and event_entity_identifiers.concept_alias = 'event'",// +
//                        //"WHERE event.data ?? :1",
//                params
//        );
//        assertEquals(expected, queryBuilder.parameterizedQuery(false));
//    }

    @Test
    public void should_write_valid_sql_for_exists_expression_multiple_columns_same_entity() {
        QueryBuilder queryBuilder = queryBuilder("event");
        queryBuilder.visit(new ExistsExpression("[col2, col3]"));

        Map<String, String> params = new HashMap<>();
        params.put("1", "urn:col2");
        params.put("2", "urn:event_col3");
        ParametrizedQuery expected = new ParametrizedQuery(
                "SELECT event.data AS \"event_data\", event_entity_identifiers.identifier AS \"event_root_identifier\" FROM project_1.event AS event " +
                        "LEFT JOIN entity_identifiers AS event_entity_identifiers ON event_entity_identifiers.expedition_id = event.expedition_id and event_entity_identifiers.concept_alias = 'event' " +
                        "WHERE event.data ??& array[:1, :2]",
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
                "SELECT event.data AS \"event_data\", event_entity_identifiers.identifier AS \"event_root_identifier\" FROM project_1.event AS event " +
                        "JOIN project_1.sample AS sample ON sample.parent_identifier = event.local_identifier and sample.expedition_id = event.expedition_id " +
                        "LEFT JOIN entity_identifiers AS event_entity_identifiers ON event_entity_identifiers.expedition_id = event.expedition_id and event_entity_identifiers.concept_alias = 'event' " +
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
                "SELECT event.data AS \"event_data\", event_entity_identifiers.identifier AS \"event_root_identifier\" FROM project_1.event AS event " +
                        "JOIN expeditions ON expeditions.id = event.expedition_id " +
                        "LEFT JOIN entity_identifiers AS event_entity_identifiers ON event_entity_identifiers.expedition_id = event.expedition_id and event_entity_identifiers.concept_alias = 'event' " +
                        "WHERE expeditions.expedition_code = :1",
                params
        );
        assertEquals(expected, queryBuilder.parameterizedQuery(false));
    }

    @Test
    public void should_write_valid_sql_for_multi_entity_expedition_expression_single_expedition() {
        QueryBuilder queryBuilder = queryBuilder("event");
        queryBuilder.visit(new SelectExpression("sample", new ExpeditionExpression("TEST")));

        Map<String, String> params = new HashMap<>();
        params.put("1", "TEST");
        ParametrizedQuery expected = new ParametrizedQuery(
                "SELECT event.data AS \"event_data\", event_entity_identifiers.identifier AS \"event_root_identifier\", sample.data AS \"sample_data\", sample_entity_identifiers.identifier AS \"sample_root_identifier\" FROM project_1.event AS event " +
                        "JOIN expeditions ON expeditions.id = event.expedition_id " +
                        "LEFT JOIN project_1.sample AS sample ON sample.parent_identifier = event.local_identifier and sample.expedition_id = event.expedition_id " +
                        "LEFT JOIN entity_identifiers AS event_entity_identifiers ON event_entity_identifiers.expedition_id = event.expedition_id and event_entity_identifiers.concept_alias = 'event' " +
                        "LEFT JOIN entity_identifiers AS sample_entity_identifiers ON sample_entity_identifiers.expedition_id = sample.expedition_id and sample_entity_identifiers.concept_alias = 'sample' " +
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
                "SELECT event.data AS \"event_data\", event_entity_identifiers.identifier AS \"event_root_identifier\" FROM project_1.event AS event " +
                        "JOIN expeditions ON expeditions.id = event.expedition_id " +
                        "LEFT JOIN entity_identifiers AS event_entity_identifiers ON event_entity_identifiers.expedition_id = event.expedition_id and event_entity_identifiers.concept_alias = 'event' " +
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
                "SELECT event.data AS \"event_data\", event_entity_identifiers.identifier AS \"event_root_identifier\" FROM project_1.event AS event " +
                        "LEFT JOIN entity_identifiers AS event_entity_identifiers ON event_entity_identifiers.expedition_id = event.expedition_id and event_entity_identifiers.concept_alias = 'event' " +
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
                "SELECT event.data AS \"event_data\", event_entity_identifiers.identifier AS \"event_root_identifier\" FROM project_1.event AS event " +
                        "LEFT JOIN entity_identifiers AS event_entity_identifiers ON event_entity_identifiers.expedition_id = event.expedition_id and event_entity_identifiers.concept_alias = 'event' " +
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
                "SELECT event.data AS \"event_data\", event_entity_identifiers.identifier AS \"event_root_identifier\" FROM project_1.event AS event " +
                        "LEFT JOIN entity_identifiers AS event_entity_identifiers ON event_entity_identifiers.expedition_id = event.expedition_id and event_entity_identifiers.concept_alias = 'event' " +
                        "WHERE event.data->>'urn:col2' ILIKE :1",
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
                "SELECT event.data AS \"event_data\", event_entity_identifiers.identifier AS \"event_root_identifier\" FROM project_1.event AS event " +
                        "LEFT JOIN entity_identifiers AS event_entity_identifiers ON event_entity_identifiers.expedition_id = event.expedition_id and event_entity_identifiers.concept_alias = 'event' " +
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

        String expectedSql = "SELECT event.data AS \"event_data\", event_entity_identifiers.identifier AS \"event_root_identifier\" FROM project_1.event AS event " +
                "LEFT JOIN entity_identifiers AS event_entity_identifiers ON event_entity_identifiers.expedition_id = event.expedition_id and event_entity_identifiers.concept_alias = 'event' " +
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
                "SELECT event.data AS \"event_data\", event_entity_identifiers.identifier AS \"event_root_identifier\" FROM project_1.event AS event " +
                        "LEFT JOIN entity_identifiers AS event_entity_identifiers ON event_entity_identifiers.expedition_id = event.expedition_id and event_entity_identifiers.concept_alias = 'event' " +
                        "WHERE (event.data->>'urn:col2' >= :1 AND event.data->>'urn:col2' <= :2)",
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
                "SELECT event.data AS \"event_data\", event_entity_identifiers.identifier AS \"event_root_identifier\" FROM project_1.event AS event " +
                        "LEFT JOIN entity_identifiers AS event_entity_identifiers ON event_entity_identifiers.expedition_id = event.expedition_id and event_entity_identifiers.concept_alias = 'event' " +
                        "WHERE (event.data->>'urn:col2' > :1 AND event.data->>'urn:col2' < :2)",
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
                "SELECT event.data AS \"event_data\", event_entity_identifiers.identifier AS \"event_root_identifier\" FROM project_1.event AS event " +
                        "LEFT JOIN entity_identifiers AS event_entity_identifiers ON event_entity_identifiers.expedition_id = event.expedition_id and event_entity_identifiers.concept_alias = 'event' " +
                        "WHERE (event.data->>'urn:col2' > :1 AND event.data->>'urn:col2' <= :2)",
                params
        );
        assertEquals(expected, queryBuilder.parameterizedQuery(false));
    }

    @Test
    public void should_write_valid_sql_for_not_range_expression_mixed_inclusive_exclusive() {
        QueryBuilder queryBuilder = queryBuilder("event");
        queryBuilder.visit(new NotExpression(new RangeExpression("col2", "{1 TO 10]")));

        Map<String, String> params = new HashMap<>();
        params.put("1", "1");
        params.put("2", "10");
        ParametrizedQuery expected = new ParametrizedQuery(
                "SELECT event.data AS \"event_data\", event_entity_identifiers.identifier AS \"event_root_identifier\" FROM project_1.event AS event " +
                        "LEFT JOIN entity_identifiers AS event_entity_identifiers ON event_entity_identifiers.expedition_id = event.expedition_id and event_entity_identifiers.concept_alias = 'event' " +
                        "WHERE not (event.data->>'urn:col2' > :1 AND event.data->>'urn:col2' <= :2)",
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
                "SELECT event.data AS \"event_data\", event_entity_identifiers.identifier AS \"event_root_identifier\" FROM project_1.event AS event " +
                        "LEFT JOIN entity_identifiers AS event_entity_identifiers ON event_entity_identifiers.expedition_id = event.expedition_id and event_entity_identifiers.concept_alias = 'event' " +
                        "WHERE (event.data->>'urn:col2' <= :1)",
                params
        );
        assertEquals(expected, queryBuilder.parameterizedQuery(false));
    }

    @Test
    public void should_write_valid_sql_for_all_expression() {
        QueryBuilder queryBuilder = queryBuilder("event");
        queryBuilder.visit(new AllExpression());

        String expectedSql = "SELECT event.data AS \"event_data\", event_entity_identifiers.identifier AS \"event_root_identifier\" FROM project_1.event AS event " +
                "LEFT JOIN entity_identifiers AS event_entity_identifiers ON event_entity_identifiers.expedition_id = event.expedition_id and event_entity_identifiers.concept_alias = 'event'";

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

        String expectedSql = "SELECT event.data AS \"event_data\", event_entity_identifiers.identifier AS \"event_root_identifier\" FROM project_1.event AS event " +
                "LEFT JOIN entity_identifiers AS event_entity_identifiers ON event_entity_identifiers.expedition_id = event.expedition_id and event_entity_identifiers.concept_alias = 'event' " +
                "WHERE " +
                "event.local_identifier <= :1 AND " +
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

        String expectedSql = "SELECT event.data AS \"event_data\", event_entity_identifiers.identifier AS \"event_root_identifier\" FROM project_1.event AS event " +
                "JOIN project_1.sample AS sample ON sample.parent_identifier = event.local_identifier and sample.expedition_id = event.expedition_id " +
                "LEFT JOIN entity_identifiers AS event_entity_identifiers ON event_entity_identifiers.expedition_id = event.expedition_id and event_entity_identifiers.concept_alias = 'event' " +
                "WHERE sample.local_identifier = :1";

        Map<String, String> params = new HashMap<>();
        params.put("1", "value");
        ParametrizedQuery expected = new ParametrizedQuery(expectedSql, params);
        assertEquals(expected, queryBuilder.parameterizedQuery(false));
    }

    @Test
    public void should_add_joins_for_multi_entity_select_query_with_direct_relationship() {
        QueryBuilder queryBuilder = queryBuilder("event");
        queryBuilder.visit(new SelectExpression("sample", new ComparisonExpression("eventId", "value", ComparisonOperator.EQUALS)));

        String expectedSql = "SELECT event.data AS \"event_data\", event_entity_identifiers.identifier AS \"event_root_identifier\", sample.data AS \"sample_data\", sample_entity_identifiers.identifier AS \"sample_root_identifier\" FROM project_1.event AS event " +
                "LEFT JOIN project_1.sample AS sample ON sample.parent_identifier = event.local_identifier and sample.expedition_id = event.expedition_id " +
                "LEFT JOIN entity_identifiers AS event_entity_identifiers ON event_entity_identifiers.expedition_id = event.expedition_id and event_entity_identifiers.concept_alias = 'event' " +
                "LEFT JOIN entity_identifiers AS sample_entity_identifiers ON sample_entity_identifiers.expedition_id = sample.expedition_id and sample_entity_identifiers.concept_alias = 'sample' " +
                "WHERE event.local_identifier = :1";

        Map<String, String> params = new HashMap<>();
        params.put("1", "value");
        ParametrizedQuery expected = new ParametrizedQuery(expectedSql, params);
        assertEquals(expected, queryBuilder.parameterizedQuery(false));
    }

    @Test
    public void should_add_joins_for_multi_entity_query_with_non_direct_relationship_child_entity_query() {
        QueryBuilder queryBuilder = queryBuilder("tissue");
        queryBuilder.visit(new ComparisonExpression("event.eventId", "1", ComparisonOperator.EQUALS));

        String expectedSql = "SELECT tissue.data AS \"tissue_data\", tissue_entity_identifiers.identifier AS \"tissue_root_identifier\" FROM project_1.tissue AS tissue " +
                "JOIN project_1.sample AS sample ON sample.local_identifier = tissue.parent_identifier and sample.expedition_id = tissue.expedition_id " +
                "JOIN project_1.event AS event ON event.local_identifier = sample.parent_identifier and event.expedition_id = sample.expedition_id " +
                "LEFT JOIN entity_identifiers AS tissue_entity_identifiers ON tissue_entity_identifiers.expedition_id = tissue.expedition_id and tissue_entity_identifiers.concept_alias = 'tissue' " +
                "WHERE event.local_identifier = :1";

        Map<String, String> params = new HashMap<>();
        params.put("1", "1");
        ParametrizedQuery expected = new ParametrizedQuery(expectedSql, params);
        assertEquals(expected, queryBuilder.parameterizedQuery(false));
    }

    @Test
    public void should_add_joins_for_multi_entity_query_with_non_direct_relationship_with_parent_entity_query() {
        QueryBuilder queryBuilder = queryBuilder("event");
        queryBuilder.visit(new ComparisonExpression("tissue.tissueId", "1", ComparisonOperator.EQUALS));

        String expectedSql = "SELECT event.data AS \"event_data\", event_entity_identifiers.identifier AS \"event_root_identifier\" FROM project_1.event AS event " +
                "JOIN project_1.sample AS sample ON sample.parent_identifier = event.local_identifier and sample.expedition_id = event.expedition_id " +
                "JOIN project_1.tissue AS tissue ON tissue.parent_identifier = sample.local_identifier and tissue.expedition_id = sample.expedition_id " +
                "LEFT JOIN entity_identifiers AS event_entity_identifiers ON event_entity_identifiers.expedition_id = event.expedition_id and event_entity_identifiers.concept_alias = 'event' " +
                "WHERE tissue.local_identifier = :1";

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

        String expectedSql = "SELECT event.data AS \"event_data\", event_entity_identifiers.identifier AS \"event_root_identifier\" FROM project_1.event AS event " +
                "JOIN project_1.sample AS sample ON sample.parent_identifier = event.local_identifier and sample.expedition_id = event.expedition_id " +
                "JOIN project_1.tissue AS tissue ON tissue.parent_identifier = sample.local_identifier and tissue.expedition_id = sample.expedition_id " +
                "LEFT JOIN entity_identifiers AS event_entity_identifiers ON event_entity_identifiers.expedition_id = event.expedition_id and event_entity_identifiers.concept_alias = 'event' " +
                "WHERE tissue.local_identifier = :1 AND sample.local_identifier = :2";

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

        Project project = new Project.ProjectBuilder("TEST", null, config)
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
        Attribute a1 = new Attribute("intCol", "urn:intCol");
        a1.setDataType(DataType.INTEGER);
        e.addAttribute(a1);
        Attribute a2 = new Attribute("floatCol", "urn:floatCol");
        a2.setDataType(DataType.FLOAT);
        e.addAttribute(a2);
        Attribute a3 = new Attribute("dateCol", "urn:dateCol");
        a3.setDataType(DataType.DATE);
        e.addAttribute(a3);
        Attribute a4 = new Attribute("datetimeCol", "urn:datetimeCol");
        a4.setDataType(DataType.DATETIME);
        e.addAttribute(a4);
        Attribute a5 = new Attribute("timeCol", "urn:timeCol");
        a5.setDataType(DataType.TIME);
        e.addAttribute(a5);
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