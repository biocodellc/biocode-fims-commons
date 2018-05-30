package biocode.fims.query;

import biocode.fims.projectConfig.models.Entity;

/**
 * @author rjewing
 */
public interface QueryBuildingExpressionVisitor extends ExpressionVisitor {
    ParametrizedQuery parameterizedQuery(boolean onlyPublicExpeditions);

    Entity entity();

    String queryTable();
}
