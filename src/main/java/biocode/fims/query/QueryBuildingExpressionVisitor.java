package biocode.fims.query;

/**
 * @author rjewing
 */
public interface QueryBuildingExpressionVisitor extends ExpressionVisitor {
    String query();
}
