package biocode.fims.repositories;

import biocode.fims.entities.Project;
import biocode.fims.repositories.customOperations.ProjectCustomOperations;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.repository.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * This repositories provides CRUD operations for {@link Project} objects
 */
@Transactional
public interface ProjectRepository extends Repository<Project, Integer>, ProjectCustomOperations {

    @Modifying
    void delete(Project project);

    void save(Project project);

    Project findByProjectId(int projectId);

    List<Project> findAll();

    List<Project> findAllByProjectUrl(String projectUrl);
}
