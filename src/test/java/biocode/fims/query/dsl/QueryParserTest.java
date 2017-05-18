package biocode.fims.query.dsl;

import biocode.fims.digester.Entity;
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
        QueryParser parser = Parboiled.createParser(QueryParser.class, queryBuilder);
        parseRunner = new ReportingParseRunner<>(parser.Parse());
    }

    @Test
    public void should_return_empty_query_given_empty_string() {
        Query result = parseRunner.run("").resultValue;
        assertEquals(new Query(queryBuilder, new EmptyExpression()), result);
    }

    @Test
    public void should_parse_simple_fts_expression() {
        String qs = " value1 ";

        Query result = parseRunner.run(qs).resultValue;

        Query expected = new Query(queryBuilder, new FTSExpression(null, "value1"));

        assertEquals(expected, result);
    }

    @Test
    public void should_parse_simple_fts_filter_expression() {
        String qs = "col1:value1 ";

        Query result = parseRunner.run(qs).resultValue;

        Query expected = new Query(queryBuilder, new FTSExpression("col1", "value1"));

        assertEquals(expected, result);
    }

    @Test
    public void should_parse_equals_filter_expression() {
        String qs = " col1 = value1";

        Query result = parseRunner.run(qs).resultValue;

        Query expected = new Query(queryBuilder, new ComparisonExpression("col1", "value1", ComparisonOperator.EQUALS));

        assertEquals(expected, result);
    }

    @Test
    public void should_parse_greater_then_filter_expression() {
        String qs = "col1 > value1";

        Query result = parseRunner.run(qs).resultValue;

        Query expected = new Query(queryBuilder, new ComparisonExpression("col1", "value1", ComparisonOperator.GREATER_THEN));

        assertEquals(expected, result);
    }

    @Test
    public void should_parse_greater_then_equal_filter_expression() {
        String qs = "col1 >=value1";

        Query result = parseRunner.run(qs).resultValue;

        Query expected = new Query(queryBuilder, new ComparisonExpression("col1", "value1", ComparisonOperator.GREATER_THEN_EQUAL));

        assertEquals(expected, result);
    }

    @Test
    public void should_parse_less_then_filter_expression() {
        String qs = "col1 < value1";

        Query result = parseRunner.run(qs).resultValue;

        Query expected = new Query(queryBuilder, new ComparisonExpression("col1", "value1", ComparisonOperator.LESS_THEN));

        assertEquals(expected, result);
    }

    @Test
    public void should_parse_less_then_equal_filter_expression() {
        String qs = "col1<=value1";

        Query result = parseRunner.run(qs).resultValue;

        Query expected = new Query(queryBuilder, new ComparisonExpression("col1", "value1", ComparisonOperator.LESS_THEN_EQUAL));

        assertEquals(expected, result);
    }

    @Test
    public void should_parse_not_equal_filter_expression() {
        String qs = "col1 <> value1";

        Query result = parseRunner.run(qs).resultValue;

        Query expected = new Query(queryBuilder, new ComparisonExpression("col1", "value1", ComparisonOperator.NOT_EQUALS));

        assertEquals(expected, result);
    }

    @Test
    public void should_parse_equals_filter_expression_with_phrase() {
        String qs = " col1 = \" some and value\\\"1\"";

        Query result = parseRunner.run(qs).resultValue;

        Query expected = new Query(queryBuilder, new ComparisonExpression("col1", " some and value\"1", ComparisonOperator.EQUALS));

        assertEquals(expected, result);
    }

    @Test
    public void should_parse_phrase_filter_expression() {
        String qs = " col1:\"some value and another\"";

        Query result = parseRunner.run(qs).resultValue;

        Query expected = new Query(queryBuilder, new LikeExpression("col1", "%some value and another%"));

        assertEquals(expected, result);
    }

    @Test
    public void should_parse_range_filter_expression() {
        String qs = " col1:[* TO 10}";

        Query result = parseRunner.run(qs).resultValue;

        Query expected = new Query(queryBuilder, new RangeExpression("col1", "[* TO 10}"));

        assertEquals(expected, result);
    }

    @Test
    public void should_parse_like_filter_expression() {
        String qs = " col1::\"%test\"";

        Query result = parseRunner.run(qs).resultValue;

        Query expected = new Query(queryBuilder, new LikeExpression("col1", "%test"));

        assertEquals(expected, result);
    }

    @Test
    public void should_parse_exists_filter_expression() {
        String qs = "_exists_:col1";

        Query result = parseRunner.run(qs).resultValue;

        Query expected = new Query(queryBuilder, new ExistsExpression("col1"));

        assertEquals(expected, result);
    }

    @Test
    public void should_parse_multiple_exists_filter_expression() {
        String qs = "_exists_:[col1, col2]";

        Query result = parseRunner.run(qs).resultValue;

        Query expected = new Query(queryBuilder, new ExistsExpression("col1, col2"));

        assertEquals(expected, result);
    }

    @Test
    public void should_parse_expedition_filter_expression() {
        String qs = "_expeditions_:exp_1";

        Query result = parseRunner.run(qs).resultValue;

        Query expected = new Query(queryBuilder, new ExpeditionExpression("exp_1"));

        assertEquals(expected, result);
    }

    @Test
    public void should_parse_multiple_expeditions_filter_expression() {
        String qs = "_expeditions_:[exp_1, exp_2]";

        Query result = parseRunner.run(qs).resultValue;

        Query expected = new Query(queryBuilder, new ExpeditionExpression("exp_1, exp_2"));

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

        Query expected = new Query(queryBuilder, l2);

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

        Query expected = new Query(queryBuilder, root);

        assertEquals(expected, result);
    }

    private Project project() {
        ProjectConfig config = new ProjectConfig();
        config.addEntity(new Entity("event"));

        Project project = new Project.ProjectBuilder("TEST", null, config, null)
                .build();

        project.setProjectId(1);

        return project;
    }
}