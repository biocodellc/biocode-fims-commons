package biocode.fims.repositories;

import biocode.fims.models.OAuthNonce;
import biocode.fims.repositories.customOperations.OAuthNonceCustomOperations;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.repository.Repository;
import org.springframework.transaction.annotation.Transactional;

/**
 * This repositories provides CRUD operations for {@link OAuthNonce} objects
 */
@Transactional
public interface OAuthNonceRepository extends Repository<OAuthNonce, Integer>, OAuthNonceCustomOperations {

    void save(OAuthNonce oAuthNonce);

    @Modifying
    void delete(OAuthNonce oAuthNonce);
}
