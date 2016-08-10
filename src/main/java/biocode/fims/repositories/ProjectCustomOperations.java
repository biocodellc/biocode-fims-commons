package biocode.fims.repositories;

import biocode.fims.entities.Project;

/**
 * defines custom ProjectRepository operations
 */
public interface ProjectCustomOperations {
    Project readByProjectId(int projectId, String entityGraph);
}
