package biocode.fims.query.dsl;

import org.elasticsearch.index.query.QueryBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author rjewing
 */
public class QueryClause implements QueryExpression, QueryContainer {
    List<QueryExpression> expressions;

    public QueryClause() {
        expressions = new ArrayList<>();
    }

    public QueryClause(List<QueryExpression> expressions) {
        this.expressions = expressions;
    }

    public void add(QueryExpression e) {
        this.expressions.add(e);
    }

    @Override
    public List<QueryBuilder> getQueryBuilders() {
        return expressions
                .stream()
                .flatMap(e -> e.getQueryBuilders().stream())
                .collect(Collectors.toList());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof QueryClause)) return false;

        QueryClause that = (QueryClause) o;

        return expressions.equals(that.expressions);
    }

    @Override
    public int hashCode() {
        return expressions.hashCode();
    }
}
