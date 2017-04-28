package biocode.fims.repositories.customOperations;

import biocode.fims.models.OAuthNonce;

/**
 * @author RJ Ewing
 */
public interface OAuthNonceCustomOperations {

    OAuthNonce getOAuthNonce(String clientId, String code, String redirectUri, long expirationInterval);

}
