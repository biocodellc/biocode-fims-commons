package biocode.fims.query;

import biocode.fims.digester.Entity;

/**
 * @author rjewing
 */
public interface QueryBuildingExpressionVisitor extends ExpressionVisitor {
    ParametrizedQuery parameterizedQuery(boolean onlyPublicExpeditions);

    Entity entity();

    String queryTable();
}
