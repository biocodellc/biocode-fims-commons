package biocode.fims.repositories;

import biocode.fims.bcid.ResourceTypes;
import biocode.fims.entities.Bcid;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

/**
 * This repositories provides CRUD operations for {@link Bcid} objects
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
    Bcid findByIdentifier(@Param("identifier") String identifier);

    Bcid findByBcidId(int bcidId);

    Bcid findByExpeditionExpeditionIdAndResourceTypeIn(int expeditionId, String... resourceType);

    @Query(value =
            "select b from Bcid b where b.ts in \n" +
                "(select max(b2.ts) from Bcid b2 where " +
                    "b2.expedition.project.projectId=:projectId and " +
                    "b2.resourceType='" + ResourceTypes.DATASET_RESOURCE_TYPE + "' " +
                    "and b.expedition.expeditionId=b2.expedition.expeditionId" +
                ")"
    )
    Set<Bcid> findLatestDatasets(@Param("projectId") int projectId);

    @Query(value =
            "select b from Bcid b where b.ts in \n" +
                    "(select max(b2.ts) from Bcid b2 where " +
                    "b2.expedition.expeditionId in (:expeditionList) and " +
                    "b2.resourceType='" + ResourceTypes.DATASET_RESOURCE_TYPE + "' " +
                    "and b.expedition.expeditionId=b2.expedition.expeditionId" +
                    ")"
    )
    Set<Bcid> findLatestDatasetsForExpeditions(@Param("expeditionList") List<Integer> expeditionList);

    void deleteByBcidId(int bcidId);

    Set<Bcid> findByExpeditionExpeditionIdAndResourceTypeNotIn(int expeditionId, String... datasetResourceType);
}
