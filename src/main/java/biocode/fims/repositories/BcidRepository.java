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
    @Query(value = "update bcids set modified=CURRENT_TIMESTAMP where id=:id", nativeQuery = true)
    void updateModifiedTs(@Param("id") int id);

    /**
     * This method invokes the SQL query that is configured by using the {@code @Query} annotation.
     *
     * @param identifier the identifier of the {@link Bcid} to fetch
     * @return the {@link Bcid} with the provided identifier
     */
    Bcid findOneByIdentifier(@Param("identifier") String identifier);

    Bcid findById(int id);

    Set<Bcid> findAllByEzidRequestTrue();

//    @Query(value = "select b from Bcid b where b.expedition.expeditionId=:expeditionId and b.resourceType in :resourceTypes")
    Bcid findByExpeditionIdAndResourceTypeIn(@Param("expeditionId") int expeditionId, @Param("resourceTypes") String... resourceType);

    @Query(value =
            "select b from Bcid b where b.id in \n" +
                    "(select max(b2.id) from Bcid b2 where " +
                    "b2.expedition.project.id=:projectId and " +
                    "b2.resourceType='" + ResourceTypes.DATASET_RESOURCE_TYPE + "' and " +
                    "b2.subResourceType='" + FimsMetadataFileManager.DATASET_RESOURCE_SUB_TYPE + "' " +
                    "and b.expedition.id=b2.expedition.id" +
                    ")"
    )
    Set<Bcid> findLatestFimsMetadataDatasets(@Param("projectId") int projectId);

    void deleteById(int id);

    List<Bcid> findByExpeditionIdAndResourceTypeNotIn(int expeditionId, String... datasetResourceType);

    Bcid findOneByTitleAndExpeditionId(String title, int expeditionId);

    Set<Bcid> findAllByEzidRequestTrueAndEzidMadeFalse();

    @Query("select b from Bcid b where b.expedition.project.id=:projectId and b.expedition.expeditionCode=:expeditionCode " +
            "and b.resourceType=:resourceType and b.subResourceType=:subResourceType order by b.created desc ")
    List<Bcid> findAllByResourceTypeAndSubResourceType(@Param("projectId") int projectId,
                                                       @Param("expeditionCode") String expeditionCode,
                                                       @Param("resourceType") String resourceType,
                                                       @Param("subResourceType") String subResourceType);

    @Query("select b from Bcid b where b.expedition.project.id=:projectId and b.expedition.expeditionCode=:expeditionCode " +
            "and b.resourceType=:resourceType order by b.created desc ")
    List<Bcid> findAllByResourceType(@Param("projectId") int projectId,
                                     @Param("expeditionCode") String expeditionCode,
                                     @Param("resourceType") String resourceType);

    List<Bcid> findAllByGraphIn(List<String> graph);

    List<Bcid> findAllByEzidRequestFalse();
}
