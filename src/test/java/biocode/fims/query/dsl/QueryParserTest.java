package biocode.fims.query.dsl;

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

    @Before
    public void setUp() throws Exception {
        QueryParser parser = Parboiled.createParser(QueryParser.class);
        parseRunner = new ReportingParseRunner<>(parser.Parse());
    }

    @Test
    public void should_return_empty_query_given_empty_string() {
        Query result = parseRunner.run("").resultValue;
        assertEquals(new Query(new EmptyExpression()), result);
    }

    @Test
    public void should_parse_simple_fts_expression() {
        String qs = " value1 ";

        Query result = parseRunner.run(qs).resultValue;

        Query expected = new Query(new FTSExpression(null, "value1"));

        assertEquals(expected, result);
    }

    @Test
    public void should_parse_simple_fts_filter_expression() {
        String qs = "col1:value1 ";

        Query result = parseRunner.run(qs).resultValue;

        Query expected = new Query(new FTSExpression("col1", "value1"));

        assertEquals(expected, result);
    }

    @Test
    public void should_parse_equals_filter_expression() {
        String qs = " col1 = value1";

        Query result = parseRunner.run(qs).resultValue;

        Query expected = new Query(new ComparisonExpression("col1", "value1", ComparisonOperator.EQUALS));

        assertEquals(expected, result);
    }

    @Test
    public void should_parse_greater_then_filter_expression() {
        String qs = "col1 > value1";

        Query result = parseRunner.run(qs).resultValue;

        Query expected = new Query(new ComparisonExpression("col1", "value1", ComparisonOperator.GREATER_THEN));

        assertEquals(expected, result);
    }

    @Test
    public void should_parse_greater_then_equal_filter_expression() {
        String qs = "col1 >=value1";

        Query result = parseRunner.run(qs).resultValue;

        Query expected = new Query(new ComparisonExpression("col1", "value1", ComparisonOperator.GREATER_THEN_EQUAL));

        assertEquals(expected, result);
    }

    @Test
    public void should_parse_less_then_filter_expression() {
        String qs = "col1 < value1";

        Query result = parseRunner.run(qs).resultValue;

        Query expected = new Query(new ComparisonExpression("col1", "value1", ComparisonOperator.LESS_THEN));

        assertEquals(expected, result);
    }

    @Test
    public void should_parse_less_then_equal_filter_expression() {
        String qs = "col1<=value1";

        Query result = parseRunner.run(qs).resultValue;

        Query expected = new Query(new ComparisonExpression("col1", "value1", ComparisonOperator.LESS_THEN_EQUAL));

        assertEquals(expected, result);
    }

    @Test
    public void should_parse_not_equal_filter_expression() {
        String qs = "col1 <> value1";

        Query result = parseRunner.run(qs).resultValue;

        Query expected = new Query(new ComparisonExpression("col1", "value1", ComparisonOperator.NOT_EQUALS));

        assertEquals(expected, result);
    }

    @Test
    public void should_parse_equals_filter_expression_with_phrase() {
        String qs = " col1 = \" some and value\\\"1\"";

        Query result = parseRunner.run(qs).resultValue;

        Query expected = new Query(new ComparisonExpression("col1", " some and value\"1", ComparisonOperator.EQUALS));

        assertEquals(expected, result);
    }

    @Test
    public void should_parse_phrase_filter_expression() {
        String qs = " col1:\"some value and another\"";

        Query result = parseRunner.run(qs).resultValue;

        Query expected = new Query(new PhraseExpression("col1", "some value and another"));

        assertEquals(expected, result);
    }

    @Test
    public void should_parse_range_filter_expression() {
        String qs = " col1:[* TO 10}";

        Query result = parseRunner.run(qs).resultValue;

        Query expected = new Query(new RangeExpression("col1", "[* TO 10}"));

        assertEquals(expected, result);
    }

    @Test
    public void should_parse_like_filter_expression() {
        String qs = " col1::\"%test\"";

        Query result = parseRunner.run(qs).resultValue;

        Query expected = new Query(new LikeExpression("col1", "%test"));

        assertEquals(expected, result);
    }

    @Test
    public void should_parse_exists_filter_expression() {
        String qs = "_exists_:col1";

        Query result = parseRunner.run(qs).resultValue;

        Query expected = new Query(new ExistsExpression("col1"));

        assertEquals(expected, result);
    }

    @Test
    public void should_parse_multiple_exists_filter_expression() {
        String qs = "_exists_:[col1, col2]";

        Query result = parseRunner.run(qs).resultValue;

        Query expected = new Query(new ExistsExpression("col1, col2"));

        assertEquals(expected, result);
    }

    @Test
    public void should_parse_expedition_filter_expression() {
        String qs = "_expeditions_:exp_1";

        Query result = parseRunner.run(qs).resultValue;

        Query expected = new Query(new ExpeditionExpression("exp_1"));

        assertEquals(expected, result);
    }

    @Test
    public void should_parse_multiple_expeditions_filter_expression() {
        String qs = "_expeditions_:[exp_1, exp_2]";

        Query result = parseRunner.run(qs).resultValue;

        Query expected = new Query(new ExpeditionExpression("exp_1, exp_2"));

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

        Query expected = new Query(l2);

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
        Expression r2 = new LogicalExpression(
                LogicalOperator.OR,
                new FTSExpression(null, "value2"),
                new FTSExpression(null, "val3")
        );
        Expression r1 = new LogicalExpression(
                LogicalOperator.AND,
                new FTSExpression(null, "val"),
                r2
        );
        Expression root = new LogicalExpression(
                LogicalOperator.OR,
                l1,
                r1
        );

        Query expected = new Query(root);

        assertEquals(expected, result);
    }
}