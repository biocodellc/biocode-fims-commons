package biocode.fims.repositories;

import biocode.fims.projectConfig.models.Entity;
import biocode.fims.projectConfig.ProjectConfig;

import java.util.List;

/**
 * @author rjewing
 */
public interface ProjectConfigRepository {

    void save(ProjectConfig config, int projectId);

    void createProjectSchema(int projectId);

    ProjectConfig getConfig(int projectId);

    void createEntityTables(List<Entity> entities, int projectId, ProjectConfig config);

    void removeEntityTables(List<Entity> entities, int projectId);
}
