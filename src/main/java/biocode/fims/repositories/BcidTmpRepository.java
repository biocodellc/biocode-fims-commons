package biocode.fims.repositories;

import biocode.fims.entities.BcidTmp;

import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.QueryByExampleExecutor;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * This repositories provides CRUD operations for {@link BcidTmp} objects
 */
@Deprecated
@Transactional
public interface BcidTmpRepository extends Repository<BcidTmp, Integer>, QueryByExampleExecutor<BcidTmp> {

    List<BcidTmp> findByExpeditionExpeditionIdAndResourceTypeNotIn(int expeditionId, String... datasetResourceType);
}
