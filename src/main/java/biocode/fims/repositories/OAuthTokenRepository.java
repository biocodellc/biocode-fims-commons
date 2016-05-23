package biocode.fims.repositories;

import biocode.fims.entities.OAuthToken;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.repository.Repository;
import org.springframework.transaction.annotation.Transactional;

/**
 * This repositories provides CRUD operations for {@link OAuthToken} objects
 */
@Transactional
public interface OAuthTokenRepository extends Repository<OAuthToken, Integer> {

    void save(OAuthToken oAuthToken);

    @Modifying
    void delete(OAuthToken oAuthToken);

    OAuthToken findOneByRefreshToken(String refreshToken);

    OAuthToken findOneByToken(String accessToken);

}
