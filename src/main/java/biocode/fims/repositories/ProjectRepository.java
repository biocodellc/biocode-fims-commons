package biocode.fims.repositories;

import biocode.fims.models.Project;
import biocode.fims.repositories.customOperations.ProjectCustomOperations;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;
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

    @Query("select case when (count(p) > 0) then true else false end from Project p join p.projectMembers pm where p.projectId=:projectId and pm.userId=:userId")
    boolean userIsMember(@Param("projectId") int projectId, @Param("userId") int userId);
}
