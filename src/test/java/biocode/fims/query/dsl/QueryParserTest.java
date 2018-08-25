package biocode.fims.query.dsl;

import biocode.fims.projectConfig.models.Entity;
import biocode.fims.models.Project;
import biocode.fims.projectConfig.ProjectConfig;
import biocode.fims.query.QueryBuilder;
import biocode.fims.query.QueryBuildingExpressionVisitor;
import org.junit.Before;
import org.junit.Test;
import org.parboiled.Parboiled;
import org.parboiled.parserunners.ParseRunner;
import org.parboiled.parserunners.ReportingParseRunner;

import static org.junit.Assert.*;

/**
 * @author rjewing
 */
public class QueryParserTest {
    ParseRunner<Query> parseRunner;
    QueryBuildingExpressionVisitor queryBuilder;

    @Before
    public void setUp() throws Exception {
        queryBuilder = new QueryBuilder(project(), "event");
        QueryParser parser = Parboiled.createParser(QueryParser.class, queryBuilder, null);
        parseRunner = new ReportingParseRunner<>(parser.Parse());
    }

    @Test
    public void should_return_empty_query_given_empty_string() {
        Query result = parseRunner.run("").resultValue;
        assertEquals(new Query(queryBuilder, null, new EmptyExpression()), result);
    }

    @Test
    public void should_parse_all_expression() {
        String qs = " * ";

        Query result = parseRunner.run(qs).resultValue;

        Query expected = new Query(queryBuilder, null, new AllExpression());

        assertEquals(expected, result);
    }

    @Test
    public void should_parse_simple_fts_expression() {
        String qs = " value1 ";

        Query result = parseRunner.run(qs).resultValue;

        Query expected = new Query(queryBuilder, null, new FTSExpression(null, "value1"));

        assertEquals(expected, result);
    }

    @Test
    public void should_parse_multiple_fts_expression() {
        String qs = "new caledonia";

        Query result = parseRunner.run(qs).resultValue;

        Query expected = new Query(queryBuilder, null, new FTSExpression(null, "new caledonia"));

        assertEquals(expected, result);
    }


    @Test
    public void should_not_parse_multiple_fts_with_column_expression() {
        String qs = "col1:new caledonia";

        Query result = parseRunner.run(qs).resultValue;

        assertEquals(null, result);
    }

    @Test
    public void should_parse_simple_not_expression() {
        String qs = "not value1 ";

        Query result = parseRunner.run(qs).resultValue;

        Query expected = new Query(queryBuilder, null, new NotExpression(new FTSExpression(null, "value1")));

        assertEquals(expected, result);
    }

    @Test
    public void should_parse_simple_fts_filter_expression() {
        String qs = "col1:event.value1 ";

        Query result = parseRunner.run(qs).resultValue;

        Query expected = new Query(queryBuilder, null, new FTSExpression("col1", "event.value1"));

        assertEquals(expected, result);
    }

    @Test
    public void should_parse_equals_filter_expression() {
        String qs = " col1 = value1";

        Query result = parseRunner.run(qs).resultValue;

        Query expected = new Query(queryBuilder, null, new ComparisonExpression("col1", "value1", ComparisonOperator.EQUALS));

        assertEquals(expected, result);
    }

    @Test
    public void should_parse_greater_then_filter_expression() {
        String qs = "col1 > value1";

        Query result = parseRunner.run(qs).resultValue;

        Query expected = new Query(queryBuilder, null, new ComparisonExpression("col1", "value1", ComparisonOperator.GREATER_THEN));

        assertEquals(expected, result);
    }

    @Test
    public void should_parse_greater_then_equal_filter_expression() {
        String qs = "col1 >=value1";

        Query result = parseRunner.run(qs).resultValue;

        Query expected = new Query(queryBuilder, null, new ComparisonExpression("col1", "value1", ComparisonOperator.GREATER_THEN_EQUAL));

        assertEquals(expected, result);
    }

    @Test
    public void should_parse_less_then_filter_expression() {
        String qs = "col1 < value1";

        Query result = parseRunner.run(qs).resultValue;

        Query expected = new Query(queryBuilder, null, new ComparisonExpression("col1", "value1", ComparisonOperator.LESS_THEN));

        assertEquals(expected, result);
    }

    @Test
    public void should_parse_less_then_equal_filter_expression() {
        String qs = "col1<=value1";

        Query result = parseRunner.run(qs).resultValue;

        Query expected = new Query(queryBuilder, null, new ComparisonExpression("col1", "value1", ComparisonOperator.LESS_THEN_EQUAL));

        assertEquals(expected, result);
    }

    @Test
    public void should_parse_not_equal_filter_expression() {
        String qs = "col1 <> value1";

        Query result = parseRunner.run(qs).resultValue;

        Query expected = new Query(queryBuilder, null, new ComparisonExpression("col1", "value1", ComparisonOperator.NOT_EQUALS));

        assertEquals(expected, result);
    }

    @Test
    public void should_parse_equals_filter_expression_with_phrase() {
        String qs = " col1 = \" some and value\\\"1\"";

        Query result = parseRunner.run(qs).resultValue;

        Query expected = new Query(queryBuilder, null, new ComparisonExpression("col1", " some and value\"1", ComparisonOperator.EQUALS));

        assertEquals(expected, result);
    }

    @Test
    public void should_parse_phrase_filter_expression() {
        String qs = " col1:\"some value and another\"";

        Query result = parseRunner.run(qs).resultValue;

        Query expected = new Query(queryBuilder, null, new LikeExpression("col1", "%some value and another%"));

        assertEquals(expected, result);
    }


    @Test
    public void should_parse_phrase_expression_preceded_by_select_expression() {
        // this was a bug that wasn't parsed
        String qs = "_select_:[Tissue,Sample,Event] Event.country:\"some value\"";

        Query result = parseRunner.run(qs).resultValue;

        Query expected = new Query(queryBuilder, null, new SelectExpression("Tissue,Sample,Event", new LikeExpression("Event.country", "%some value%")));

        assertEquals(expected, result);
    }

    @Test
    public void should_parse_range_filter_expression() {
        String qs = " col1:[* TO 10}";

        Query result = parseRunner.run(qs).resultValue;

        Query expected = new Query(queryBuilder, null, new RangeExpression("col1", "[* TO 10}"));

        assertEquals(expected, result);
    }

    @Test
    public void should_parse_like_filter_expression() {
        String qs = " col1::\"%test\"";

        Query result = parseRunner.run(qs).resultValue;

        Query expected = new Query(queryBuilder, null, new LikeExpression("col1", "%test"));

        assertEquals(expected, result);
    }

    @Test
    public void should_parse_exists_filter_expression() {
        String qs = "_exists_:col1";

        Query result = parseRunner.run(qs).resultValue;

        Query expected = new Query(queryBuilder, null, new ExistsExpression("col1"));

        assertEquals(expected, result);
    }

    @Test
    public void should_parse_path_based_exists_filter_expression() {
        String qs = "_exists_:event.col1";

        Query result = parseRunner.run(qs).resultValue;

        Query expected = new Query(queryBuilder, null, new ExistsExpression("event.col1"));

        assertEquals(expected, result);
    }

    @Test
    public void should_parse_multiple_exists_filter_expression() {
        String qs = "_exists_:[col1, col2]";

        Query result = parseRunner.run(qs).resultValue;

        Query expected = new Query(queryBuilder, null, new ExistsExpression("col1, col2"));

        assertEquals(expected, result);
    }

    @Test
    public void should_parse_expedition_filter_expression() {
        String qs = "_expeditions_:exp_1";

        Query result = parseRunner.run(qs).resultValue;

        Query expected = new Query(queryBuilder, null, new ExpeditionExpression("exp_1"));

        assertEquals(expected, result);
    }

    @Test
    public void should_parse_multiple_expeditions_filter_expression() {
        String qs = "_expeditions_:[exp_1, exp_2]";

        Query result = parseRunner.run(qs).resultValue;

        Query expected = new Query(queryBuilder, null, new ExpeditionExpression("exp_1, exp_2"));

        assertEquals(expected, result);
    }

    @Test
    public void should_parse_select_with_filter_expression() {
        String qs = "_select_:entity _exists_:col1";

        Query result = parseRunner.run(qs).resultValue;

        Query expected = new Query(queryBuilder, null, new SelectExpression("entity", new ExistsExpression("col1")));

        assertEquals(expected, result);
    }

    @Test
    public void should_not_parse_select_in_and_or_or_expression() {
        String qs1 = "_exists_:col1 and _select_:entity";

        Query result = parseRunner.run(qs1).resultValue;
        assertEquals(null, result);

        String qs2 = "_select_:entity and _exists_:col1";
        result = parseRunner.run(qs1).resultValue;
        assertEquals(null, result);

        String qs3 = "_exists_:col1 or _select_:entity";

        result = parseRunner.run(qs3).resultValue;
        assertEquals(null, result);

        String qs4 = "_select_:entity or _exists_:col1";
        result = parseRunner.run(qs4).resultValue;
        assertEquals(null, result);
    }

    // TODO this would be nice to solve, currently it returns a FTS for not & SelectExpression
//    @Test
//    public void should_not_parse_select_in_not_expression() {
//        String qs = "not _select_:entity";
//
//        Query result = parseRunner.run(qs).resultValue;
//        assertEquals(null, result);
//    }
//
    @Test
    public void should_parse_select_filter_expression() {
        String qs = "_select_:entity";

        Query result = parseRunner.run(qs).resultValue;

        Query expected = new Query(queryBuilder, null, new SelectExpression("entity", new AllExpression()));

        assertEquals(expected, result);
    }

    @Test
    public void should_parse_multiple_select_filter_expression() {
        String qs = "_select_:[e_1, e_2]";

        Query result = parseRunner.run(qs).resultValue;

        Query expected = new Query(queryBuilder, null, new SelectExpression("e_1, e_2", new AllExpression()));

        assertEquals(expected, result);
    }

    @Test
    public void should_parse_multiple_separate_select_with_filter_expression() {
        String qs = "_select_:entity _exists_:col1 _select_:e2";

        Query result = parseRunner.run(qs).resultValue;

        Query expected = new Query(queryBuilder, null, new SelectExpression("entity,e2", new ExistsExpression("col1")));

        assertEquals(expected, result);
    }

    @Test
    public void should_parse_multiple_seperate_select_without_filter_expression() {
        String qs = "_select_:entity _select_:e2";

        Query result = parseRunner.run(qs).resultValue;

        Query expected = new Query(queryBuilder, null, new SelectExpression("entity,e2", new AllExpression()));

        assertEquals(expected, result);
    }

    @Test
    public void should_parse_multiple_and_expression() {
        String qs = "col2 = someValue and col1::\"%test\" and value2";

        Query result = parseRunner.run(qs).resultValue;

        Expression l1 = new LogicalExpression(
                LogicalOperator.AND,
                new ComparisonExpression("col2", "someValue", ComparisonOperator.EQUALS),
                new LikeExpression("col1", "%test")
        );
        Expression l2 = new LogicalExpression(
                LogicalOperator.AND,
                l1,
                new FTSExpression(null, "value2")
        );

        Query expected = new Query(queryBuilder, null, l2);

        assertEquals(expected, result);
    }

    @Test
    public void should_parse_multiple_and_expression_failing_query() {
        String qs = "_exists_:decimalLongitude and _exists_:decimalLatitude and _exists_:permitInformation";

        Query result = parseRunner.run(qs).resultValue;

        Expression l1 = new LogicalExpression(
                LogicalOperator.AND,
                new ExistsExpression("decimalLongitude"),
                new ExistsExpression("decimalLatitude")
        );
        Expression l2 = new LogicalExpression(
                LogicalOperator.AND,
                l1,
                new ExistsExpression("permitInformation")
        );

        Query expected = new Query(queryBuilder, null, l2);

        assertEquals(expected, result);
    }

    @Test
    public void should_parse_and_or_expression() {
        String qs = "col2 = someValue and col1::\"%test\" or val and (value2 or val3)";

        Query result = parseRunner.run(qs).resultValue;

        Expression l1 = new LogicalExpression(
                LogicalOperator.AND,
                new ComparisonExpression("col2", "someValue", ComparisonOperator.EQUALS),
                new LikeExpression("col1", "%test")
        );
        Expression g1 = new GroupExpression(
                new LogicalExpression(
                        LogicalOperator.OR,
                        new FTSExpression(null, "value2"),
                        new FTSExpression(null, "val3")
                )
        );
        Expression r1 = new LogicalExpression(
                LogicalOperator.AND,
                new FTSExpression(null, "val"),
                g1
        );
        Expression root = new LogicalExpression(
                LogicalOperator.OR,
                l1,
                r1
        );

        Query expected = new Query(queryBuilder, null, root);

        assertEquals(expected, result);
    }

    @Test
    public void should_not_parse_double_not_expression() {
        String qs = "not not value1 ";

        Query result = parseRunner.run(qs).resultValue;

        assertEquals(null, result);
    }

    @Test
    public void should_parse_complex_not_expression() {
        String qs = "col2 = someValue and not col1::\"%test\" or val and not (value2 or not val3)";

        Query result = parseRunner.run(qs).resultValue;

        Expression l1 = new LogicalExpression(
                LogicalOperator.AND,
                new ComparisonExpression("col2", "someValue", ComparisonOperator.EQUALS),
                new NotExpression(new LikeExpression("col1", "%test"))
        );
        Expression g1 = new GroupExpression(
                new LogicalExpression(
                        LogicalOperator.OR,
                        new FTSExpression(null, "value2"),
                        new NotExpression(new FTSExpression(null, "val3"))
                )
        );
        Expression r1 = new LogicalExpression(
                LogicalOperator.AND,
                new FTSExpression(null, "val"),
                new NotExpression(g1)
        );
        Expression root = new LogicalExpression(
                LogicalOperator.OR,
                l1,
                r1
        );

        Query expected = new Query(queryBuilder, null, root);

        assertEquals(expected, result);
    }

    private Project project() {
        ProjectConfig config = new ProjectConfig();
        config.addEntity(new Entity("event", "someURI"));

        Project project = new Project.ProjectBuilder("TEST", null, config)
                .build();

        project.setProjectId(1);

        return project;
    }
}