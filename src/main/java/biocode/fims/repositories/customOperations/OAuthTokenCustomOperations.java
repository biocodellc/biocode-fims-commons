package biocode.fims.repositories.customOperations;

import biocode.fims.entities.OAuthToken;
import biocode.fims.entities.User;

/**
 * defines custom OAuthTokenRepository operations
 */
public interface OAuthTokenCustomOperations {
    User getUser(String accessToken, long expirationInterval);

    OAuthToken getOAuthToken(String refreshToken, long expirationInteval);
}
