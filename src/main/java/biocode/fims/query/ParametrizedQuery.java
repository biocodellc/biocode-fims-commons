package biocode.fims.query;

import org.springframework.util.Assert;

import java.util.Map;

/**
 * @author rjewing
 */
public class ParametrizedQuery {
    private final String sql;
    private final Map<String, String> params;

    public ParametrizedQuery(String sql, Map<String, String> params) {
        Assert.notNull(sql);
        Assert.notNull(params);
        this.sql = sql;
        this.params = params;
    }

    public String sql() {
        return sql;
    }

    public Map<String, String> params() {
        return params;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ParametrizedQuery)) return false;

        ParametrizedQuery that = (ParametrizedQuery) o;

        if (!sql.equals(that.sql)) return false;
        return params.equals(that.params);
    }

    @Override
    public int hashCode() {
        int result = sql.hashCode();
        result = 31 * result + params.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return "ParametrizedQuery{" +
                "sql='" + sql + '\'' +
                ", params=" + params +
                '}';
    }
}
