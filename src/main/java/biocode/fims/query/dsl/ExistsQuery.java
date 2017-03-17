package biocode.fims.query.dsl;

import biocode.fims.elasticSearch.FieldColumnTransformer;
import biocode.fims.elasticSearch.query.ElasticSearchFilterField;
import org.apache.lucene.search.join.ScoreMode;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;

import java.util.Arrays;
import java.util.List;

/**
 * @author rjewing
 */
public class ExistsQuery implements QueryExpression {
    private String column;
    private FieldColumnTransformer transformer;

    public ExistsQuery(String column) {
        this.column = column;
    }

    @Override
    public List<QueryBuilder> getQueryBuilders() {
        ElasticSearchFilterField filterField = transformer.getFilterField(column);

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
        return QueryBuilders.existsQuery(filterField.getField());
    }


    @Override
    public void setColumn(FieldColumnTransformer transformer, String column) {
        this.transformer = transformer;
        this.column = column;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ExistsQuery)) return false;

        ExistsQuery that = (ExistsQuery) o;

        if (!column.equals(that.column)) return false;
        return transformer != null ? transformer.equals(that.transformer) : that.transformer == null;
    }

    @Override
    public int hashCode() {
        int result = column.hashCode();
        result = 31 * result + (transformer != null ? transformer.hashCode() : 0);
        return result;
    }
}
