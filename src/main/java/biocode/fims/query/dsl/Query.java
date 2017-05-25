package biocode.fims.query.dsl;


import biocode.fims.digester.Entity;
import biocode.fims.query.ExpeditionCollectingExpressionVisitor;
import biocode.fims.query.ParametrizedQuery;
import biocode.fims.query.QueryBuildingExpressionVisitor;
import org.springframework.util.Assert;

import java.util.List;
import java.util.Set;

/**
 * @author rjewing
 */
public class Query {

    private final QueryBuildingExpressionVisitor queryBuilder;
    private final Expression expression;
    private Set<String> expeditions;

    public Query(QueryBuildingExpressionVisitor queryBuilder, Expression expression) {
        Assert.notNull(queryBuilder);
        Assert.notNull(expression);
        this.queryBuilder = queryBuilder;
        this.expression = expression;
    }

    public ParametrizedQuery parameterizedQuery(boolean onlyPublicExpeditions) {
        expression.accept(queryBuilder);
        return queryBuilder.parameterizedQuery(onlyPublicExpeditions);
    }

    public Set<String> expeditions() {
        if (expeditions == null) {
            ExpeditionCollectingExpressionVisitor visitor = new ExpeditionCollectingExpressionVisitor();
            expression.accept(visitor);
            expeditions = visitor.expeditions();
        }

        return expeditions;
    }

    public Entity entity() {
        return queryBuilder.entity();
    }

    public String queryTable() {
        return queryBuilder.queryTable();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Query)) return false;

        Query query = (Query) o;

        if (!queryBuilder.equals(query.queryBuilder)) return false;
        return expression.equals(query.expression);
    }

    @Override
    public int hashCode() {
        int result = queryBuilder.hashCode();
        result = 31 * result + expression.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return expression.toString();
    }
}
