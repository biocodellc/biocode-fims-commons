package biocode.fims.repositories;

import biocode.fims.bcid.ResourceTypes;
import biocode.fims.entities.Bcid;

import biocode.fims.fileManagers.fimsMetadata.FimsMetadataFileManager;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;
import org.springframework.data.repository.query.QueryByExampleExecutor;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

/**
 * This repositories provides CRUD operations for {@link Bcid} objects
 */
@Transactional
public interface BcidRepository extends Repository<Bcid, Integer>, QueryByExampleExecutor<Bcid> {

    @Modifying
    void delete(Bcid bcid);

    void save(Bcid bcid);

    @Modifying
    @Query(value = "update bcids set modified=CURRENT_TIMESTAMP where bcidId=:bcidId", nativeQuery = true)
    void updateModifiedTs(@Param("bcidId") int bcidId);

    /**
     * This method invokes the SQL query that is configured by using the {@code @Query} annotation.
     *
     * @param identifier the identifier of the {@link Bcid} to fetch
     * @return the {@link Bcid} with the provided identifier
     */
    Bcid findOneByIdentifier(@Param("identifier") String identifier);

    Bcid findByBcidId(int bcidId);

    Set<Bcid> findAllByEzidRequestTrue();

    Bcid findByExpeditionExpeditionIdAndResourceTypeIn(int expeditionId, String... resourceType);

    @Query(value =
            "select b from Bcid b where b.bcidId in \n" +
                    "(select max(b2.bcidId) from Bcid b2 where " +
                    "b2.expedition.project.projectId=:projectId and " +
                    "b2.resourceType='" + ResourceTypes.DATASET_RESOURCE_TYPE + "' and " +
                    "b2.subResourceType='" + FimsMetadataFileManager.DATASET_RESOURCE_SUB_TYPE + "' " +
                    "and b.expedition.expeditionId=b2.expedition.expeditionId" +
                    ")"
    )
    Set<Bcid> findLatestFimsMetadataDatasets(@Param("projectId") int projectId);

    void deleteByBcidId(int bcidId);

    List<Bcid> findByExpeditionExpeditionIdAndResourceTypeNotIn(int expeditionId, String... datasetResourceType);

    Bcid findOneByTitleAndExpeditionExpeditionId(String title, int expeditionId);

    Set<Bcid> findAllByEzidRequestTrueAndEzidMadeFalse();

    @Query("select b from Bcid b where b.expedition.project.projectId=:projectId and b.expedition.expeditionCode=:expeditionCode " +
            "and b.resourceType=:resourceType and b.subResourceType=:subResourceType order by b.created desc ")
    List<Bcid> findAllByResourceTypeAndSubResourceType(@Param("projectId") int projectId,
                                                       @Param("expeditionCode") String expeditionCode,
                                                       @Param("resourceType") String resourceType,
                                                       @Param("subResourceType") String subResourceType);

    @Query("select b from Bcid b where b.expedition.project.projectId=:projectId and b.expedition.expeditionCode=:expeditionCode " +
            "and b.resourceType=:resourceType order by b.created desc ")
    List<Bcid> findAllByResourceType(@Param("projectId") int projectId,
                                     @Param("expeditionCode") String expeditionCode,
                                     @Param("resourceType") String resourceType);

    List<Bcid> findAllByGraphIn(List<String> graph);

    List<Bcid> findAllByEzidRequestFalse();
}
