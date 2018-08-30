package biocode.fims.repositories.customOperations;

import biocode.fims.config.project.models.PersistedProjectConfig;
import biocode.fims.models.Project;
import org.springframework.data.repository.query.Param;

import java.util.List;

/**
 * defines custom ProjectRepository operations
 */
public interface ProjectCustomOperations {
    Project getProjectByProjectId(int projectId, String entityGraph);

    List<Project> getAll(List<Integer> projectIds, String entityGraph);

    PersistedProjectConfig getConfig(@Param("projectId") int projectId);
}
