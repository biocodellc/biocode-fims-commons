package biocode.fims.query.dsl;

import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;

import java.util.Arrays;
import java.util.List;

/**
 * @author rjewing
 */
public class ExistsQuery implements FieldQueryExpression {
    private String column;

    public ExistsQuery(String column) {
        this.column = column;
    }

    @Override
    public List<QueryBuilder> getQueryBuilders() {
        return Arrays.asList(
                QueryBuilders.existsQuery(column)
        );
    }

    @Override
    public void setColumn(String column) {
        this.column = column;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ExistsQuery)) return false;

        ExistsQuery that = (ExistsQuery) o;

        return column.equals(that.column);
    }

    @Override
    public int hashCode() {
        return column.hashCode();
    }
}
