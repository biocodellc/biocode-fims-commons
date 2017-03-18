package biocode.fims.query.dsl;

import biocode.fims.elasticSearch.FieldColumnTransformer;
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
    Query expected;
    FieldColumnTransformer transformer;

    @Before
    public void setUp() throws Exception {
        transformer = null;
        QueryParser parser = Parboiled.createParser(QueryParser.class, transformer);
        parseRunner = new ReportingParseRunner<>(parser.Parse());
        expected = new Query();
    }

    @Test
    public void should_return_empty_query_given_empty_string() {
        Query result = parseRunner.run("").resultValue;
        assertEquals(expected, result);
    }

    @Test
    public void should_parse_single_query_string() {
        String qs = "multiple term query \"with phrases\"";

        Query result = parseRunner.run(qs).resultValue;

        expected.add(new QueryStringQuery("multiple"));
        expected.add(new QueryStringQuery("term"));
        expected.add(new QueryStringQuery("query"));
        expected.add(new QueryStringQuery("\"with phrases\""));

        assertEquals(expected, result);
    }

    @Test
    public void should_parse_exists_column() {
        String qs = " _exists_:column1";

        Query result = parseRunner.run(qs).resultValue;

        expected.add(new ExistsQuery(transformer, "column1"));

        assertEquals(expected, result);
    }

    @Test
    public void should_parse_query_with_multiple_exists_and_query_strings() {
        String qs = " _exists_:column1 this term _exists_:column3 \"query string\" phrase";

        Query result = parseRunner.run(qs).resultValue;

        expected.add(new ExistsQuery(transformer, "column1"));
        expected.add(new QueryStringQuery("this"));
        expected.add(new QueryStringQuery("term"));
        expected.add(new ExistsQuery(transformer, "column3"));
        expected.add(new QueryStringQuery("\"query string\""));
        expected.add(new QueryStringQuery("phrase"));

        assertEquals(expected, result);

    }

    @Test
    public void should_parse_must_queries() {
        String qs = "+term1 shouldTerm +_exists_:column1 +\"phrase must\"";

        Query result = parseRunner.run(qs).resultValue;

        QueryClause must = new QueryClause();
        must.add(new QueryStringQuery("term1"));
        expected.addMust(must);

        expected.add(new QueryStringQuery("shouldTerm"));

        QueryClause must2 = new QueryClause();
        must2.add(new ExistsQuery(transformer, "column1"));
        expected.addMust(must2);

        QueryClause must3 = new QueryClause();
        must3.add(new QueryStringQuery("\"phrase must\""));
        expected.addMust(must3);

        assertEquals(expected, result);
    }

    @Test
    public void should_parse_must_not_queries() {
        String qs = "-term1 -(shouldTerm orTerm) -_exists_:column1 +\"phrase must\"";

        Query result = parseRunner.run(qs).resultValue;

        QueryClause mustNot = new QueryClause();
        mustNot.add(new QueryStringQuery("term1"));
        expected.addMustNot(mustNot);

        Query group = new Query();
        group.add(new QueryStringQuery("shouldTerm"));
        group.add(new QueryStringQuery("orTerm"));
        QueryClause mustNot2 = new QueryClause();
        mustNot2.add(group);
        expected.addMustNot(mustNot2);

        QueryClause mustNot3 = new QueryClause();
        mustNot3.add(new ExistsQuery(transformer, "column1"));
        expected.addMustNot(mustNot3);

        QueryClause must = new QueryClause();
        must.add(new QueryStringQuery("\"phrase must\""));
        expected.addMust(must);

        assertEquals(expected, result);
    }

    @Test
    public void should_parse_sub_queries() {
        String qs = "term1 ( shouldTerm ) term";

        Query result = parseRunner.run(qs).resultValue;

        expected.add(new QueryStringQuery("term1"));

        Query group = new Query();
        group.add(new QueryStringQuery("shouldTerm"));
        expected.add(group);

        expected.add(new QueryStringQuery("term"));

        assertEquals(expected, result);
    }

    @Test
    public void should_parse_filter_queries() {
        String qs = "_exists_:column1 column2:term1 term2 column3:(term3 +term4)";

        Query result = parseRunner.run(qs).resultValue;

        expected.add(new ExistsQuery(transformer, "column1"));

        QueryStringQuery q = new QueryStringQuery("term1");
        q.setColumn(transformer, "column2");
        expected.add(q);

        expected.add(new QueryStringQuery("term2"));

        Query group = new Query();
        group.setColumn(transformer, "column3");
        group.add(new QueryStringQuery("term3"));
        QueryClause must = new QueryClause();
        must.add(new QueryStringQuery("term4"));
        group.addMust(must);
        expected.add(group);

        assertEquals(expected, result);
    }

    @Test
    public void should_parse_range_queries() {
        String qs = "column1:[1 TO 5] column2:<=5 column3:{5 TO 10]";

        Query result = parseRunner.run(qs).resultValue;

        QueryStringQuery q = new QueryStringQuery("[1 TO 5]");
        q.setColumn(transformer, "column1");
        expected.add(q);

        QueryStringQuery q2 = new QueryStringQuery("<=5");
        q2.setColumn(transformer, "column2");
        expected.add(q2);

        QueryStringQuery q3 = new QueryStringQuery("{5 TO 10]");
        q3.setColumn(transformer, "column3");
        expected.add(q3);

        assertEquals(expected, result);
    }

    @Test
    public void should_parse_filter_with_dot_notation() {
        String qs = "conceptAlias.\\*:a";

        Query result = parseRunner.run(qs).resultValue;

        QueryStringQuery q = new QueryStringQuery("a");
        q.setColumn(transformer, "conceptAlias.\\*");
        expected.add(q);

        assertEquals(expected, result);
    }

    @Test
    public void should_parse_expeditons() {
        String qs = "term expedition:TEST expedition:TEST2";

        Query result = parseRunner.run(qs).resultValue;

        expected.add(new QueryStringQuery("term"));

        expected.addExpedition("TEST");
        expected.addExpedition("TEST2");

        assertEquals(expected, result);
    }

    @Test
    public void should_parse_escaped_must_not_char_as_query_string() {
        String qs = "decimalLatitude:<\\-10";

        Query result = parseRunner.run(qs).resultValue;

        QueryStringQuery q = new QueryStringQuery("<\\-10");
        q.setColumn(transformer, "decimalLatitude");

        expected.add(q);

        assertEquals(expected, result);
    }

    @Test
    public void should_parse_bounding_box_query() {
        String qs = "(+decimalLongitude:>170 +decimalLongitude:<=180) (+decimalLongitude:<\\-170 +decimalLongitude:>=\\-180) +expedition:Panpen_COI_MI";

        Query result = parseRunner.run(qs).resultValue;

        Query group = new Query();

        QueryClause groupMust1 = new QueryClause();
        QueryStringQuery q = new QueryStringQuery(">170");
        q.setColumn(transformer, "decimalLongitude");
        groupMust1.add(q);
        group.addMust(groupMust1);

        QueryClause groupMust2 = new QueryClause();
        QueryStringQuery q2 = new QueryStringQuery("<=180");
        q2.setColumn(transformer, "decimalLongitude");
        groupMust2.add(q2);
        group.addMust(groupMust2);

        expected.add(group);

        Query group2 = new Query();

        QueryClause group2Must1 = new QueryClause();
        QueryStringQuery q3 = new QueryStringQuery("<\\-170");
        q3.setColumn(transformer, "decimalLongitude");
        group2Must1.add(q3);
        group2.addMust(group2Must1);

        QueryClause group2Must2 = new QueryClause();
        QueryStringQuery q4 = new QueryStringQuery(">=\\-180");
        q4.setColumn(transformer, "decimalLongitude");
        group2Must2.add(q4);
        group2.addMust(group2Must2);

        expected.add(group2);

        QueryClause must = new QueryClause();
        must.addExpedition("Panpen_COI_MI");
        expected.addMust(must);

        assertEquals(expected, result);
    }

}