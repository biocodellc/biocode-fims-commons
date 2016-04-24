package biocode.fims.intelliJEntities;

import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

/**
 * This repository provides CRUD operations for {@link Bcid} objects
 */
@Transactional
public interface BcidRepository extends Repository<Bcid, Integer>, JpaSpecificationExecutor {

    @Modifying
    void delete(Bcid bcid);

    void save(Bcid bcid);

    /**
     * This method invokes the SQL query that is configured by using the {@code @Query} annotation.
     * @param identifier the identifier of the {@link Bcid} to fetch
     * @return the {@link Bcid} with the provided identifier
     */
    @Query(value =
            "SELECT b.bcidId, b.ezidMade, b.ezidRequest, b.identifier, b.userId, b.doi, b.title, " +
                    "b.webAddress, b.resourceType, b.ts, b.graph, b.finalCopy, eb.expeditionId " +
            "FROM bcids AS b " +
            "LEFT OUTER JOIN expeditionBcids AS eb " +
                    "ON b.bcidId=eb.bcidId " +
            "WHERE BINARY identifier=:identifier",
            nativeQuery = true)
//     TODO catch EmptyResultDataAccessException an throw bad request with invalid identifier?
    Bcid findByIdentifier(@Param("identifier") String identifier);

    Bcid findByBcidId(int bcidId);

    /**
     * @param expeditionId the {@link biocode.fims.entities.Expedition} the bcids are associated with
     * @param resourceType the resourceType(s) of the Bcids to find
     * @return the {@link Bcid} Set associated with the provided {@link biocode.fims.entities.Expedition}, containing
     * the provided resourceType(s)
     */
    Set<Bcid> findByExpeditionExpeditionIdAndResourceTypeIn(int expeditionId, String... resourceType);
}
