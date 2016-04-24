package biocode.fims.intelliJEntities;

import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.repository.Repository;
import org.springframework.transaction.annotation.Transactional;

/**
 * This repository provides CRUD operations for {@link User} objects
 */
@Transactional
public interface ExpeditionRepository extends Repository<Expedition, Integer>, JpaSpecificationExecutor {

    @Modifying
    void delete(Expedition expedition);

    void save(Expedition expedition);

    Expedition findByExpeditionId(int expeditionId);

//    /**
//     * @param expeditionId the {@link biocode.fims.entities.Expedition} the bcids are associated with
//     * @param resourceType the resourceType(s) of the Bcids to find
//     * @return the {@link Bcid} Set associated with the provided {@link biocode.fims.entities.Expedition}, containing
//     * the provided resourceType(s)
//     */
//    Set<Bcid> findByExpeditionExpeditionIdAndResourceTypeIn(int expeditionId, String... resourceType);
}
