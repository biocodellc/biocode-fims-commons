package biocode.fims.repositories;

import biocode.fims.entities.Project;

import java.util.List;

/**
 * defines custom ProjectRepository operations
 */
public interface ProjectCustomOperations {
    Project getProjectByProjectId(int projectId, String entityGraph);

    List<Project> getAllByProjectUrl(String projectUrl, String entityGraph);
}
