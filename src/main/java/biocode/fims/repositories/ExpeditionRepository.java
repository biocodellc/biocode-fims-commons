package biocode.fims.repositories;

import biocode.fims.models.Expedition;
import biocode.fims.models.User;
import biocode.fims.repositories.customOperations.ExpeditionCustomOperations;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.util.List;

/**
 * This repositories provides CRUD operations for {@link User} objects
 */
@Transactional
public interface ExpeditionRepository extends Repository<Expedition, Integer>, JpaSpecificationExecutor, ExpeditionCustomOperations {

    @Modifying
    void delete(Expedition expedition);

    @Modifying
    void deleteByExpeditionId(int expeditionId);

    @Modifying
    void deleteByExpeditionCodeAndProjectProjectId(String expeditionCode, int projectId);

    Expedition save(Expedition expedition);

    Expedition findByExpeditionId(int expeditionId);

    Expedition findByExpeditionCodeAndProjectProjectId(String expeditionCode, int projectId);

    /**
     * return a paginated result of Expeditions
     * @param projectId
     * @param userId
     * @param pageRequest
     * @return
     */
    Page<Expedition> findByProjectProjectIdAndProjectUserUserId(int projectId, int userId, Pageable pageRequest);

    List<Expedition> findByPublicTrueAndProjectProjectId(int projectId);


    /**
     * select all public {@link Expedition} for a user in the specified project
     * @param projectId
     * @param userId
     * @param includePrivate if true, we will include private expeditions in the results
     * @return
     */
    @Query("select e from Expedition e where project_id=:projectId and user_id=:userId and (public='true' or public!=:includePrivate)")
    List<Expedition> getUserProjectExpeditions(@Param("projectId") int projectId, @Param("userId") int userId, @Param("includePrivate") boolean includePrivate);


    Long countByExpeditionIdInAndProjectProjectId(List<Integer> expeditionIds, int projectId);

    @Query("select e from Expedition e where project_id=:projectId and (public='true' or public!=:includePrivate)")
    List<Expedition> getProjectExpeditions(@Param("projectId") int projectId, @Param("includePrivate") boolean includePrivate);

    Expedition findByIdentifier(URI identifier);
}
