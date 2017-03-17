package biocode.fims.query.dsl;

/**
 * @author rjewing
 */
public class QueryFilter {

    private String column;
    private DeprecatedQuery query;

    public QueryFilter(String column, DeprecatedQuery query) {
        this.column = column;
        this.query = query;
    }

    public String getColumn() {
        return column;
    }

    public DeprecatedQuery getQuery() {
        return query;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof QueryFilter)) return false;

        QueryFilter filter = (QueryFilter) o;

        if (!getColumn().equals(filter.getColumn())) return false;
        return getQuery().equals(filter.getQuery());
    }

    @Override
    public int hashCode() {
        int result = getColumn().hashCode();
        result = 31 * result + getQuery().hashCode();
        return result;
    }
}
