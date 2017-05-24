package biocode.fims.query;

import biocode.fims.digester.Entity;

/**
 * @author rjewing
 */
public interface QueryBuildingExpressionVisitor extends ExpressionVisitor {
    ParametrizedQuery parameterizedQuery();

    Entity entity();

    String queryTable();
}
