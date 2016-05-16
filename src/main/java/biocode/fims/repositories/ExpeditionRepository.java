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

/**
 * This repositories provides CRUD operations for {@link User} objects
 */
@Transactional
public interface ExpeditionRepository extends Repository<Expedition, Integer>, JpaSpecificationExecutor {

    @Modifying
    void delete(Expedition expedition);

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

}
