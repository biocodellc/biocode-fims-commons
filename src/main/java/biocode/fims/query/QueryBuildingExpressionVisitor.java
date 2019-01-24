package biocode.fims.query;

import biocode.fims.config.models.Entity;
import biocode.fims.config.project.ProjectConfig;

/**
 * @author rjewing
 */
public interface QueryBuildingExpressionVisitor extends ExpressionVisitor {
    ParametrizedQuery parameterizedQuery(boolean onlyPublicExpeditions);

    Entity entity();

    String queryTable();

    Integer page();

    Integer limit();

    void setProjectConfig(ProjectConfig config);
}
