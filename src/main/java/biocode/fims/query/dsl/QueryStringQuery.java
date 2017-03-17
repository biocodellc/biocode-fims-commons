package biocode.fims.query.dsl;

import biocode.fims.elasticSearch.FieldColumnTransformer;
import biocode.fims.elasticSearch.query.ElasticSearchFilterField;
import org.apache.lucene.search.join.ScoreMode;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * @author rjewing
 */
public class QueryStringQuery implements QueryExpression {
    private String column;
    private FieldColumnTransformer transformer;
    private String queryString;

    public QueryStringQuery(String queryString) {
        this.queryString = queryString;
        this.column = "_all";
    }

    //TODO refactor out common code with ExistsQuery
    @Override
    public List<QueryBuilder> getQueryBuilders() {
        ElasticSearchFilterField filterField;

        if (transformer != null) {
            filterField = transformer.getFilterField(column);
        } else {
            filterField = new ElasticSearchFilterField("_all", null, null, null);
        }

        QueryBuilder qb = getQueryBuilder(filterField);

        if (filterField.isNested()) {
            qb = QueryBuilders.nestedQuery(
                    filterField.getPath(),
                    qb,
                    ScoreMode.None
            );
        }

        return Arrays.asList(qb);
    }

    private QueryBuilder getQueryBuilder(ElasticSearchFilterField filterField) {
        return QueryBuilders
                .queryStringQuery(queryString)
                .defaultField(filterField.getField())
                .allowLeadingWildcard(false);
    }

    @Override
    public void setColumn(FieldColumnTransformer transformer, String column) {
        this.transformer = transformer;
        this.column = column;
    }

    @Override
    public List<QueryExpression> getExpressions(String column) {
        if (this.column.equals(column)) {
            return Arrays.asList(this);
        }
        return Collections.emptyList();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof QueryStringQuery)) return false;

        QueryStringQuery that = (QueryStringQuery) o;

        if (!column.equals(that.column)) return false;
        return queryString.equals(that.queryString);
    }

    @Override
    public int hashCode() {
        int result = column.hashCode();
        result = 31 * result + queryString.hashCode();
        return result;
    }

    public String getQueryString() {
        return queryString;
    }
}
