package biocode.fims.query.dsl;

import biocode.fims.elasticSearch.FieldColumnTransformer;
import org.apache.commons.lang.StringUtils;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @author rjewing
 */
public class Query implements QueryExpression, ExpeditionQueryContainer {
    protected List<QueryClause> must;
    protected List<QueryClause> mustNot;
    protected List<QueryClause> should;
    private List<String> expeditions;

    private FieldColumnTransformer transformer;
    private String column;
    private boolean adjustMinShouldMatch = false;

    public Query() {
        must = new ArrayList<>();
        mustNot = new ArrayList<>();
        should = new ArrayList<>();
        expeditions = new ArrayList<>();
    }

    public void addMust(QueryClause q) {
        must.add(q);
        expeditions.addAll(q.getExpeditions());
    }

    public void addMustNot(QueryClause q) {
        mustNot.add(q);
        expeditions.addAll(q.getExpeditions());
    }

    public void addShould(QueryClause q) {
        should.add(q);
        expeditions.addAll(q.getExpeditions());
    }

    public void addExpedition(String expeditionCode) {
        expeditions.add(expeditionCode);
    }

    @Override
    public void add(QueryExpression q) {
        should.add(
                new QueryClause(
                        Arrays.asList(q)
                )
        );
    }

    @Override
    public List<QueryBuilder> getQueryBuilders() {
        return Arrays.asList(getQueryBuilder());
    }

    public QueryBuilder getQueryBuilder() {
        if (needToSetQueryClauseColumn()) {
            setColumnsForQueryClauses();
        }

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

        if (expeditions.size() > 0) {
            queryBuilder.must(buildExpeditionsQuery());

            if (adjustMinShouldMatch && must.size() == 0 && mustNot.size() == 0) {
                // default es behavior is that if only should queries are present, at least 1 needs to match.
                // since we add all public expeditions if no expeditions are in the query string (to prevent query results returning private expeditions),
                // we need to mimic the default es behavior
                queryBuilder.minimumNumberShouldMatch(1);
            }
        }

        return queryBuilder;
    }

    private QueryBuilder buildExpeditionsQuery() {
        BoolQueryBuilder queryBuilder = QueryBuilders.boolQuery();

        for (String expeditionCode : expeditions) {
            queryBuilder.should(
                    //TODO centralize expedition.expeditionCode.keyword
                    QueryBuilders.matchQuery("expedition.expeditionCode.keyword", expeditionCode)
            );
        }

        queryBuilder.minimumNumberShouldMatch(1);

        return queryBuilder;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Query)) return false;

        Query query = (Query) o;

        if (!must.equals(query.must)) return false;
        if (!mustNot.equals(query.mustNot)) return false;
        return should.equals(query.should);
    }

    @Override
    public int hashCode() {
        int result = must.hashCode();
        result = 31 * result + mustNot.hashCode();
        result = 31 * result + should.hashCode();
        return result;
    }

    @Override
    public void setColumn(FieldColumnTransformer transformer, String column) {
        this.transformer = transformer;
        this.column = column;
    }

    private void setColumnsForQueryClauses() {
        must.forEach(qc -> qc.setColumn(transformer, column));
        mustNot.forEach(qc -> qc.setColumn(transformer, column));
        should.forEach(qc -> qc.setColumn(transformer, column));
    }

    private boolean needToSetQueryClauseColumn() {
        return transformer != null && !StringUtils.isBlank(column);
    }

    public List<String> getExpeditions() {
        return expeditions;
    }

    public List<QueryExpression> getExpressions(String column) {
        List<QueryExpression> expressions = new ArrayList<>();

//        if (column.equals(this.column)) {
            must.forEach(qc -> expressions.addAll(qc.getExpressions(column)));
            mustNot.forEach(qc -> expressions.addAll(qc.getExpressions(column)));
            should.forEach(qc -> expressions.addAll(qc.getExpressions(column)));
//        }

        return expressions;
    }

    public void setExpeditions(List<String> expeditions) {
        this.expeditions = expeditions;
        this.adjustMinShouldMatch = true;
    }
}
