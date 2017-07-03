package biocode.fims.repositories;

import biocode.fims.bcid.ResourceTypes;
import biocode.fims.entities.BcidTmp;

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
 * This repositories provides CRUD operations for {@link BcidTmp} objects
 */
@Transactional
public interface BcidTmpRepository extends Repository<BcidTmp, Integer>, QueryByExampleExecutor<BcidTmp> {

    @Modifying
    void delete(BcidTmp bcidTmp);

    void save(BcidTmp bcidTmp);

    @Modifying
    @Query(value = "update bcids set ts=CURRENT_TIMESTAMP where bcidId=:bcidId", nativeQuery = true)
    void updateTs(@Param("bcidId") int bcidId);

    /**
     * This method invokes the SQL query that is configured by using the {@code @Query} annotation.
     *
     * @param identifier the identifier of the {@link BcidTmp} to fetch
     * @return the {@link BcidTmp} with the provided identifier
     */
    @Query(value =
            "SELECT b.bcidId, b.ezidMade, b.ezidRequest, b.identifier, b.userId, b.doi, b.title, " +
                    "b.webAddress, b.resourceType, b.ts, b.graph, b.finalCopy, eb.expeditionId, b.sourceFile, b.subResourceType " +
                    "FROM bcids AS b " +
                    "LEFT OUTER JOIN expeditionBcids AS eb " +
                    "ON b.bcidId=eb.bcidId " +
                    "WHERE BINARY identifier=:identifier",
            nativeQuery = true)
    BcidTmp findByIdentifier(@Param("identifier") String identifier);

    BcidTmp findByBcidId(int bcidId);

    Set<BcidTmp> findAllByEzidRequestTrue();

    BcidTmp findByExpeditionExpeditionIdAndResourceTypeIn(int expeditionId, String... resourceType);

    @Query(value =
            "select b from BcidTmp b where b.bcidId in \n" +
                    "(select max(b2.bcidId) from BcidTmp b2 where " +
                    "b2.expedition.project.projectId=:projectId and " +
                    "b2.resourceType='" + ResourceTypes.DATASET_RESOURCE_TYPE + "' and " +
                    "b2.subResourceType='" + FimsMetadataFileManager.DATASET_RESOURCE_SUB_TYPE + "' " +
                    "and b.expedition.expeditionId=b2.expedition.expeditionId" +
                    ")"
    )
    Set<BcidTmp> findLatestFimsMetadataDatasets(@Param("projectId") int projectId);

    void deleteByBcidId(int bcidId);

    List<BcidTmp> findByExpeditionExpeditionIdAndResourceTypeNotIn(int expeditionId, String... datasetResourceType);

    BcidTmp findOneByTitleAndExpeditionExpeditionId(String title, int expeditionId);

    Set<BcidTmp> findAllByEzidRequestTrueAndEzidMadeFalse();

    @Query("select b from BcidTmp b where b.expedition.project.projectId=:projectId and b.expedition.expeditionCode=:expeditionCode " +
            "and b.resourceType=:resourceType and b.subResourceType=:subResourceType order by b.ts desc ")
    List<BcidTmp> findAllByResourceTypeAndSubResourceType(@Param("projectId") int projectId,
                                                          @Param("expeditionCode") String expeditionCode,
                                                          @Param("resourceType") String resourceType,
                                                          @Param("subResourceType") String subResourceType);

    @Query("select b from BcidTmp b where b.expedition.project.projectId=:projectId and b.expedition.expeditionCode=:expeditionCode " +
            "and b.resourceType=:resourceType order by b.ts desc ")
    List<BcidTmp> findAllByResourceType(@Param("projectId") int projectId,
                                        @Param("expeditionCode") String expeditionCode,
                                        @Param("resourceType") String resourceType);

    List<BcidTmp> findAllByGraphIn(List<String> graph);

    List<BcidTmp> findAllByEzidRequestFalse();
}
