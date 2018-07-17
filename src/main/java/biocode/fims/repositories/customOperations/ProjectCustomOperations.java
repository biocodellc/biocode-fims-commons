package biocode.fims.repositories.customOperations;

import biocode.fims.models.Project;

import java.util.List;

/**
 * defines custom ProjectRepository operations
 */
public interface ProjectCustomOperations {
    Project getProjectByProjectId(int projectId, String entityGraph);

    List<Project> getAll(String entityGraph);
}
