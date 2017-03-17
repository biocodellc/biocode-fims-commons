package biocode.fims.query.dsl;

import biocode.fims.elasticSearch.FieldColumnTransformer;
import org.elasticsearch.index.query.QueryBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author rjewing
 */
public class QueryClause implements QueryContainer, QueryExpression {
    List<QueryExpression> expressions;
    private FieldColumnTransformer transformer;
    private String column;

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
        setColumnsForQueryExpressions();
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

    @Override
    public void setColumn(FieldColumnTransformer transformer, String column) {
        this.transformer = transformer;
        this.column = column;
    }

    private void setColumnsForQueryExpressions() {
        expressions.forEach(e -> e.setColumn(transformer, column));
    }
}
