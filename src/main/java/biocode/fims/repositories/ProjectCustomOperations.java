package biocode.fims.repositories;

import biocode.fims.entities.Project;

import java.util.List;

/**
 * defines custom ProjectRepository operations
 */
public interface ProjectCustomOperations {
    Project readByProjectId(int projectId, String entityGraph);

    List<Project> readByProjectUrl(String projectUrl, String entityGraph);
}
