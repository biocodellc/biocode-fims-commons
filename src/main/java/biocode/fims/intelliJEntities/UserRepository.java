package biocode.fims.intelliJEntities;

import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

/**
 * This repository provides CRUD operations for {@link User} objects
 */
@Transactional
public interface UserRepository extends Repository<User, Integer>, JpaSpecificationExecutor {

    @Modifying
    void delete(User user);

    void save(User user);

    User findByUserId(int userId);

//    /**
//     * @param expeditionId the {@link biocode.fims.entities.Expedition} the bcids are associated with
//     * @param resourceType the resourceType(s) of the Bcids to find
//     * @return the {@link Bcid} Set associated with the provided {@link biocode.fims.entities.Expedition}, containing
//     * the provided resourceType(s)
//     */
//    Set<Bcid> findByExpeditionExpeditionIdAndResourceTypeIn(int expeditionId, String... resourceType);
}
