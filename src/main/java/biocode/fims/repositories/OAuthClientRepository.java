package biocode.fims.repositories;

import biocode.fims.models.OAuthClient;
import org.springframework.data.repository.Repository;
import org.springframework.transaction.annotation.Transactional;

/**
 * This repositories provides CRUD operations for {@link OAuthClient} objects
 */
@Transactional
public interface OAuthClientRepository extends Repository<OAuthClient, Integer> {

    void save(OAuthClient oAuthClient);

    OAuthClient findOneByClientId(String clientId);

    OAuthClient findOneByClientIdAndClientSecret(String clientId, String clientSecret);
}
