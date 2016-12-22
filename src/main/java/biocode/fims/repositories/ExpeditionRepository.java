package biocode.fims.repositories;

import biocode.fims.entities.Expedition;
import biocode.fims.entities.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

/**
 * This repositories provides CRUD operations for {@link User} objects
 */
@Transactional
public interface ExpeditionRepository extends Repository<Expedition, Integer>, JpaSpecificationExecutor {

    @Modifying
    void delete(Expedition expedition);

    @Modifying
    void deleteByExpeditionId(int expeditionId);

    void save(Expedition expedition);

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
    @Query("select e from Expedition e where projectId=:projectId and userId=:userId and (public=true or public!=:includePrivate)")
    List<Expedition> getUserProjectExpeditions(@Param("projectId") int projectId, @Param("userId") int userId, @Param("includePrivate") boolean includePrivate);

}
