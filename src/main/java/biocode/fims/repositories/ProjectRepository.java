package biocode.fims.repositories;

import biocode.fims.entities.Project;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

/**
 * This repositories provides CRUD operations for {@link Project} objects
 */
@Transactional
public interface ProjectRepository extends Repository<Project, Integer> {

    @Modifying
    void delete(Project project);

    void save(Project project);

    Project findByProjectId(int projectId);

    @EntityGraph(value = "withMembers", type = EntityGraph.EntityGraphType.FETCH)
//    @Query("select Project from Project where projectId = :projectId")
    Project readByProjectId(@Param("projectId") int projectId);
}
