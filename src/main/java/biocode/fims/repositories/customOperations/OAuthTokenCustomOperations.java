package biocode.fims.repositories.customOperations;

import biocode.fims.models.OAuthToken;
import biocode.fims.models.User;

/**
 * defines custom OAuthTokenRepository operations
 */
public interface OAuthTokenCustomOperations {
    User getUser(String accessToken, long expirationInterval, String userEntityGraph);

    OAuthToken getOAuthToken(String refreshToken, long expirationInteval);
}
