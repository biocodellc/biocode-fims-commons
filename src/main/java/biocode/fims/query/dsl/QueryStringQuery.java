package biocode.fims.query.dsl;

import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;

import java.util.Arrays;
import java.util.List;

/**
 * @author rjewing
 */
public class QueryStringQuery implements FieldQueryExpression {
    private String column;
    private String queryString;

    public QueryStringQuery(String queryString) {
        this.queryString = queryString;
        this.column = "";
    }

    @Override
    public List<QueryBuilder> getQueryBuilders() {
        return Arrays.asList(
                QueryBuilders
                        .queryStringQuery(queryString)
                        .defaultField(column)
        );
    }

    @Override
    public void setColumn(String column) {
        this.column = column;
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
}
