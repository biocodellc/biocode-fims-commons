package biocode.fims.query.dsl;

import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @author rjewing
 */
public abstract class BoolQuery implements QueryExpression, QueryContainer {
    protected List<QueryClause> must;
    protected List<QueryClause> mustNot;
    protected List<QueryClause> should;
    private List<String> expeditions;

    public BoolQuery() {
        must = new ArrayList<>();
        mustNot = new ArrayList<>();
        should = new ArrayList<>();
        expeditions = new ArrayList<>();
    }

    public void addMust(QueryClause q) {
        must.add(q);
    }

    public void addMustNot(QueryClause q) {
        mustNot.add(q);
    }

    public void addShould(QueryClause q) {
        should.add(q);
    }

    public void addExpedition(String expeditionCode) { expeditions.add(expeditionCode); }

    @Override
    public void add(QueryExpression q) {
        should.add(
                new QueryClause(
                        Arrays.asList(q)
                )
        );
    }

    public QueryBuilder getQueryBuilder() {
        BoolQueryBuilder queryBuilder = QueryBuilders.boolQuery();

        for (QueryExpression q : must) {
            for (QueryBuilder qb : q.getQueryBuilders()) {
                queryBuilder.must(qb);
            }
        }

        for (QueryExpression q : mustNot) {
            for (QueryBuilder qb : q.getQueryBuilders()) {
                queryBuilder.mustNot(qb);
            }
        }

        for (QueryExpression q : should) {
            for (QueryBuilder qb : q.getQueryBuilders()) {
                queryBuilder.should(qb);
            }
        }

        return queryBuilder;
    }

    @Override
    public List<QueryBuilder> getQueryBuilders() {
        return Arrays.asList(getQueryBuilder());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof BoolQuery)) return false;

        BoolQuery boolQuery = (BoolQuery) o;

        if (!must.equals(boolQuery.must)) return false;
        if (!mustNot.equals(boolQuery.mustNot)) return false;
        return should.equals(boolQuery.should);
    }

    @Override
    public int hashCode() {
        int result = must.hashCode();
        result = 31 * result + mustNot.hashCode();
        result = 31 * result + should.hashCode();
        return result;
    }
}
