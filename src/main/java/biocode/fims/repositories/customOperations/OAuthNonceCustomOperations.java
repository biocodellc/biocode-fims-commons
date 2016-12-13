package biocode.fims.repositories.customOperations;

import biocode.fims.entities.OAuthNonce;

/**
 * @author RJ Ewing
 */
public interface OAuthNonceCustomOperations {

    OAuthNonce getOAuthNonce(String clientId, String code, String redirectUri, long expirationInterval);

}
