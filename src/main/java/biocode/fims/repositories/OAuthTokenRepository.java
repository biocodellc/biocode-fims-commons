package biocode.fims.repositories;

import biocode.fims.models.OAuthToken;
import biocode.fims.repositories.customOperations.OAuthTokenCustomOperations;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.transaction.annotation.Transactional;

/**
 * This repositories provides CRUD operations for {@link OAuthToken} objects
 */
@Transactional
public interface OAuthTokenRepository extends Repository<OAuthToken, Integer>, OAuthTokenCustomOperations {

    void save(OAuthToken oAuthToken);

    @Modifying
    void delete(OAuthToken oAuthToken);

    @Query("DELETE FROM OAuthToken t WHERE t.token=:token OR t.refreshToken=:token")
    void invalidate(String token);
}

