package biocode.fims.query.dsl;

import org.junit.Before;
import org.junit.Test;
import org.parboiled.Parboiled;
import org.parboiled.parserunners.ReportingParseRunner;
import org.parboiled.support.ParsingResult;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.*;

/**
 * @author rjewing
 */
public class QueryParserTest {
    QueryParser parser;

    @Before
    public void setUp() throws Exception {
        parser = Parboiled.createParser(QueryParser.class);
    }

    @Test
    public void should_return_empty_query_given_empty_string() {
        ParsingResult<Query> result = new ReportingParseRunner<Query>(parser.Parse()).run("");
        assertEquals("", result.resultValue.getQueryString());
    }

    @Test
    public void should_parse_single_query_string() {
        String qs = "multiple term query \"with phrases\"";

        ParsingResult<Query> result = new ReportingParseRunner<Query>(parser.Parse()).run(qs);

        assertEquals(qs, result.resultValue.getQueryString());
    }

    @Test
    public void should_parse_exists_column() {
        String qs = " _exists_:column1";

        ParsingResult<Query> result = new ReportingParseRunner<Query>(parser.Parse()).run(qs);

        assertEquals(Collections.singletonList("column1"), result.resultValue.getExists());
    }

    @Test
    public void should_parse_query_with_multiple_exists_and_query_strings() {
        String qs = " _exists_:column1 this term _exists_:column3 \"query string\" phrase";

        Query result = new ReportingParseRunner<Query>(parser.Parse()).run(qs).resultValue;

        List<String> expectedExists = Arrays.asList("column1", "column3");

        assertEquals("this term \"query string\" phrase", result.getQueryString());
        assertEquals(expectedExists, result.getExists());

    }

    @Test
    public void should_parse_must_queries() {
        String qs = "+term1 shouldTerm +_exists_:column1 +\"phrase must\"";

        Query result = new ReportingParseRunner<Query>(parser.Parse()).run(qs).resultValue;

        Query expectedMust1 = new Query();
        expectedMust1.appendQueryString("term1");
        Query expectedMust2 = new Query();
        expectedMust2.addExists("column1");
        Query expectedMust3 = new Query();
        expectedMust3.appendQueryString("\"phrase must\"");


        assertEquals(Arrays.asList(expectedMust1, expectedMust2, expectedMust3), result.getMust());
        assertEquals("shouldTerm", result.getQueryString());
    }

    @Test
    public void should_parse_must_not_queries() {
        String qs = "-term1 -(shouldTerm orTerm) -_exists_:column1 +\"phrase must\"";

        Query result = new ReportingParseRunner<Query>(parser.Parse()).run(qs).resultValue;

        Query mustNot1 = new Query();
        mustNot1.appendQueryString("term1");
        Query mustNot2 = new Query();
        Query groupQuery = new Query();
        groupQuery.appendQueryString("shouldTerm orTerm");
        mustNot2.addShould(groupQuery);
        Query mustNot3 = new Query();
        mustNot3.addExists("column1");
        Query must = new Query();
        must.appendQueryString("\"phrase must\"");

        Query expected = new Query();
        expected.addMustNot(mustNot1);
        expected.addMustNot(mustNot2);
        expected.addMustNot(mustNot3);
        expected.addMust(must);


        assertEquals(expected, result);
    }

    @Test
    public void should_parse_sub_queries() {
        String qs = "term1 ( shouldTerm ) term";

        ParsingResult<Query> result = new ReportingParseRunner<Query>(parser.Parse()).run(qs);

        Query expected = new Query();
        expected.appendQueryString("term1 term");
        Query subQuery = new Query();
        subQuery.appendQueryString("shouldTerm");
        expected.addShould(subQuery);

        assertEquals(expected, result.resultValue);
    }

    @Test
    public void should_parse_filter_queries() {
        String qs = "_exists_:column1 column2:term1 term2 column3:(term3 +term4)";

        Query result = new ReportingParseRunner<Query>(parser.Parse()).run(qs).resultValue;

        Query f1 = new Query();
        f1.appendQueryString("term1");
        QueryFilter filter1 = new QueryFilter("column2", f1);

        Query f2 = new Query();
        Query f2Sub = new Query();
        f2Sub.appendQueryString("term3");
        Query f2SubMust = new Query();
        f2SubMust.appendQueryString("term4");
        f2Sub.addMust(f2SubMust);
        f2.addShould(f2Sub);

        QueryFilter filter2 = new QueryFilter("column3", f2);

        Query expected = new Query();
        expected.appendQueryString("term2");
        expected.addExists("column1");
        expected.addFilter(filter1);
        expected.addFilter(filter2);

        assertEquals(expected, result);
    }

    @Test
    public void should_parse_range_queries() {
        String qs = "column1:[1 TO 5] column2:<=5 column3:{5 TO 10]";

        Query result = new ReportingParseRunner<Query>(parser.Parse()).run(qs).resultValue;

        Query f = new Query();
        f.appendQueryString("[1 TO 5]");
        QueryFilter filter = new QueryFilter("column1", f);
        Query f2 = new Query();
        f2.appendQueryString("<=5");
        QueryFilter filter2 = new QueryFilter("column2", f2);
        Query f3 = new Query();
        f3.appendQueryString("{5 TO 10]");
        QueryFilter filter3 = new QueryFilter("column3", f3);

        Query expected = new Query();
        expected.addFilter(filter);
        expected.addFilter(filter2);
        expected.addFilter(filter3);

        assertEquals(expected, result);
    }

    @Test
    public void should_parse_filter_with_dot_notation() {
        String qs = "conceptAlias.\\*:a";

        Query result = new ReportingParseRunner<Query>(parser.Parse()).run(qs).resultValue;

        Query f = new Query();
        f.appendQueryString("a");
        QueryFilter filter = new QueryFilter("conceptAlias.\\*", f);

        Query expected = new Query();
        expected.addFilter(filter);

        assertEquals(expected, result);
    }

    @Test
    public void should_parse_expeditons() {
        String qs = "term expedition:TEST expedition:TEST2";

        Query result = new ReportingParseRunner<Query>(parser.Parse()).run(qs).resultValue;

        Query expected = new Query();
        expected.addExpedition("TEST");
        expected.addExpedition("TEST2");
        expected.appendQueryString("term");

        assertEquals(expected, result);
    }

}