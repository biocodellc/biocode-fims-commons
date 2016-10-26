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

    List<Expedition> findAllByProjectProjectIdAndUserUserId(int projectId, int userId);

    List<Expedition> findByPublicTrueAndProjectProjectId(int projectId);


    /**
     * select all {@link Expedition} for a given project that the user owns or is a public expeditions
     * @param projectId
     * @param userId
     * @return
     */
    @Query("select e from Expedition e where projectId=:projectId and (userId=:userId or public=true)")
    List<Expedition> findAllForProjectAndUserOrPublic(int projectId, int userId);
}
