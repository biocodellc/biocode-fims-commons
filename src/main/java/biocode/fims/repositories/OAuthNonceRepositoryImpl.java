package biocode.fims.repositories;

import biocode.fims.models.OAuthNonce;
import biocode.fims.repositories.customOperations.OAuthNonceCustomOperations;

import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.PersistenceContext;

/**
 * Implementation of ProjectCustomOperations
 */
public class OAuthNonceRepositoryImpl implements OAuthNonceCustomOperations {
    @PersistenceContext(unitName = "entityManagerFactory")
    private EntityManager em;

    @Override
    public OAuthNonce getOAuthNonce(String clientId, String code, String redirectUri, long expirationInterval) {
        try {
            Object o = em.createNamedQuery("OAuthNonce.getOAuthNonce")
                    .setParameter("clientId", clientId)
                    .setParameter("code", code)
                    .setParameter("redirectUri", redirectUri)
                    .setParameter("expirationInterval", expirationInterval)
                    .getSingleResult();

            return (OAuthNonce) ((Object[]) o)[0];
        } catch (NoResultException e) {
            return null;
        }
    }
}
