package biocode.fims.repositories;

import biocode.fims.projectConfig.ProjectConfig;

/**
 * @author rjewing
 */
public interface ProjectConfigRepository {

    /**
     * This is a temporary method to help with the migration to postgres. This will prevent creating bcids for entities
     * that are already registered
     * @param config
     * @param projectId
     * @param checkForExistingBcids
     */
    @Deprecated
    void save(ProjectConfig config, int projectId, boolean checkForExistingBcids);

    void save(ProjectConfig config, int projectId);

    void createProjectSchema(int projectId);
}
