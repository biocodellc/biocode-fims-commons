package biocode.fims.repositories;

import biocode.fims.models.ProjectConfiguration;
import biocode.fims.models.ProjectConfiguration;
import biocode.fims.repositories.customOperations.ProjectConfigurationCustomOperations;
import biocode.fims.repositories.customOperations.ProjectCustomOperations;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * This repositories provides CRUD operations for {@link ProjectConfiguration} objects
 */
@Transactional
public interface ProjectConfigurationRepository extends Repository<ProjectConfiguration, Integer>, ProjectConfigurationCustomOperations {

    @Modifying
    @Query("delete from ProjectConfiguration where id = :id and networkApproved = false and (select count(p) from Project p where p.projectConfiguration.id = :id) = 0")
    void deleteIfNoProjects(int id);
    
    ProjectConfiguration save(ProjectConfiguration projectConfiguration);

    ProjectConfiguration findById(int id);

    List<ProjectConfiguration> findAll();

    List<ProjectConfiguration> findAllByUserUserId(int userId);

    List<ProjectConfiguration> findAllByNetworkApproved(boolean networkApproved);

    List<ProjectConfiguration> findAllByNetworkApprovedOrUserUserId(boolean networkApproved, int userId);
}
