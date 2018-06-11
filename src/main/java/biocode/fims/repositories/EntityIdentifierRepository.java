package biocode.fims.repositories;

import biocode.fims.models.EntityIdentifier;
import biocode.fims.models.Expedition;
import biocode.fims.models.User;
import biocode.fims.repositories.customOperations.ExpeditionCustomOperations;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.util.List;

/**
 * This repositories provides CRUD operations for {@link EntityIdentifier} objects
 */
@Transactional
public interface EntityIdentifierRepository extends Repository<EntityIdentifier, Integer>, JpaSpecificationExecutor {

    EntityIdentifier findByIdentifier(URI identifier);
}
