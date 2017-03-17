package biocode.fims.query.dsl;

import biocode.fims.elasticSearch.query.ElasticSearchFilterField;
import biocode.fims.fimsExceptions.FimsRuntimeException;
import biocode.fims.fimsExceptions.errorCodes.QueryErrorCode;
import org.apache.lucene.search.join.ScoreMode;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.QueryStringQueryBuilder;

import java.util.ArrayList;
import java.util.List;

/**
 * @author rjewing
 */
public class DeprecatedQuery {

    private StringBuilder queryString;
    private List<String> exists;
    private List<QueryFilter> filters;
    private List<DeprecatedQuery> must;
    private List<DeprecatedQuery> mustNot;
    private List<DeprecatedQuery> should;
    private List<String> expeditions;
    private String column = null;

    public DeprecatedQuery() {
        this.queryString = new StringBuilder();
        this.exists = new ArrayList<>();
        this.filters = new ArrayList<>();
        this.should = new ArrayList<>();
        this.must = new ArrayList<>();
        this.mustNot = new ArrayList<>();
        this.expeditions = new ArrayList<>();
    }

    public void addSubQuery(DeprecatedQuery query) {
        should.add(query);
    }

    public void setColumn(String column) {
        this.column = column;
    }
    // TODO clean this up when we refactor the configuration/query logic
    public QueryBuilder getEsQuery(List<ElasticSearchFilterField> filterFields) {
        return getEsQuery(filterFields, null);
    }

    private QueryBuilder getEsQuery(List<ElasticSearchFilterField> filterFields, String defaultColumn) {
        BoolQueryBuilder queryBuilder = QueryBuilders.boolQuery();

//        if (expeditions.size() > 0) {
//            BoolQueryBuilder expeditionsQuery = QueryBuilders.boolQuery();
//
//            for (String expedition : expeditions) {
//                // query the keyword sub-field for an exact match
//                expeditionsQuery.should(QueryBuilders.matchQuery("expedition.expeditionCode.keyword", expedition));
//                expeditionsQuery.minimumNumberShouldMatch(1);
//            }
//
//            queryBuilder.must(expeditionsQuery);
//        }

        if (!getQueryString().isEmpty()) {
            QueryStringQueryBuilder qsQuery = QueryBuilders.queryStringQuery(getQueryString());
            if (defaultColumn != null) {
                qsQuery.defaultField(defaultColumn);
            }
            queryBuilder.should(qsQuery);
        }

        for (DeprecatedQuery query : should) {
            queryBuilder.should(query.getEsQuery(filterFields));
        }

        for (String column : exists) {
            ElasticSearchFilterField field = lookupFilterField(column, filterFields);

            if (field.isNested()) {
                queryBuilder.should(
                        QueryBuilders.nestedQuery(
                                field.getPath(),
                                QueryBuilders.existsQuery(field.getField()),
                                ScoreMode.None
                        )
                );
            } else {
                queryBuilder.should(
                        QueryBuilders.existsQuery(field.getField())
                );
            }
        }

        for (DeprecatedQuery query : must) {
            queryBuilder.must(query.getEsQuery(filterFields));
        }

        for (DeprecatedQuery query : mustNot) {
            queryBuilder.mustNot(query.getEsQuery(filterFields));
        }

        for (QueryFilter filter : filters) {
            ElasticSearchFilterField field = lookupFilterField(filter.getColumn(), filterFields);

            if (field.isNested()) {
                queryBuilder.should(
                        QueryBuilders.nestedQuery(
                                field.getPath(),
                                filter.getQuery().getEsQuery(filterFields, field.getField()),
                                ScoreMode.None
                        )
                );
            } else {
                queryBuilder.should(
                        filter.getQuery().getEsQuery(filterFields, field.getField())
                );
            }
        }

        queryBuilder.minimumNumberShouldMatch(1);

        return queryBuilder;
    }

    public void addExists(String column) {
        exists.add(column);
    }

    public void addFilter(QueryFilter filter) {
        filters.add(filter);
    }

    public void addMust(DeprecatedQuery mustQuery) {
        must.add(mustQuery);
    }

    public void addMustNot(DeprecatedQuery mustNotQuery) {
        mustNot.add(mustNotQuery);
    }

    public void addShould(DeprecatedQuery subQuery) {
        this.should.add(subQuery);
    }

    public void appendQueryString(String q) {
        queryString.append(" " + q);
    }

    public void addExpedition(String expeditionCode) {
        this.expeditions.add(expeditionCode);
    }

    public List<String> getExists() {
        return exists;
    }

    public String getQueryString() {
        return queryString.toString().trim();
    }

    public List<DeprecatedQuery> getMust() {
        return must;
    }

    public List<String> getExpeditions() {
        return expeditions;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof DeprecatedQuery)) return false;

        DeprecatedQuery query = (DeprecatedQuery) o;

        if (!getQueryString().equals(query.getQueryString())) return false;
        if (!getExists().equals(query.getExists())) return false;
        if (!filters.equals(query.filters)) return false;
        if (!getMust().equals(query.getMust())) return false;
        if (!mustNot.equals(query.mustNot)) return false;
        if (!should.equals(query.should)) return false;
        return getExpeditions().equals(query.getExpeditions());
    }

    @Override
    public int hashCode() {
        int result = getQueryString().hashCode();
        result = 31 * result + getExists().hashCode();
        result = 31 * result + filters.hashCode();
        result = 31 * result + getMust().hashCode();
        result = 31 * result + mustNot.hashCode();
        result = 31 * result + should.hashCode();
        result = 31 * result + getExpeditions().hashCode();
        return result;
    }

    private ElasticSearchFilterField lookupFilterField(String column, List<ElasticSearchFilterField> filterFields) {
        for (ElasticSearchFilterField filter : filterFields) {
            if (column.equals(filter.getDisplayName())) {
                return filter;
            }
        }

        throw new FimsRuntimeException(QueryErrorCode.UNKNOWN_FILTER, "is " + column + " a filterable field?", 400, column);
    }

    public void merge(DeprecatedQuery query) {
        if (column != null) {
            throw new FimsRuntimeException("Can't merge queries. Already have a columns set", 500, null);
        }
        appendQueryString(query.getQueryString());
        exists.addAll(query.getExists());
        must.addAll(query.getMust());
        mustNot.addAll(query.mustNot);
        should.addAll(query.should);
        expeditions.addAll(query.getExpeditions());

    }
}
