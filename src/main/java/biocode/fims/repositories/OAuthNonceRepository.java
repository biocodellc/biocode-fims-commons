package biocode.fims.repositories;

import biocode.fims.entities.OAuthNonce;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.repository.Repository;
import org.springframework.transaction.annotation.Transactional;

/**
 * This repositories provides CRUD operations for {@link OAuthNonce} objects
 */
@Transactional
public interface OAuthNonceRepository extends Repository<OAuthNonce, Integer> {

    void save(OAuthNonce oAuthNonce);

    OAuthNonce findOneByCodeAndRedirectUriAndOAuthClientClientId(String clientId, String code, String redirectUri);

    OAuthNonce findOneByCodeAndOAuthClientClientId(String clientId, String code);

    @Modifying
    void delete(OAuthNonce oAuthNonce);
}
