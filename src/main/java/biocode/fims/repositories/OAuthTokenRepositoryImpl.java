package biocode.fims.repositories;

import biocode.fims.entities.OAuthToken;
import biocode.fims.entities.User;
import biocode.fims.repositories.customOperations.OAuthTokenCustomOperations;

import javax.persistence.EntityGraph;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.PersistenceContext;

/**
 * Implementation of ProjectCustomOperations
 */
public class OAuthTokenRepositoryImpl implements OAuthTokenCustomOperations {
    @PersistenceContext(unitName = "entityManagerFactory")
    private EntityManager em;

    @Override
    public User getUser(String accessToken, long expirationInterval) {
        try {
            return (User) em.createNamedQuery("OAuthToken.getUser")
                    .setParameter("token", accessToken)
                    .setParameter("expirationInterval", expirationInterval)
                    .getSingleResult();
        } catch (NoResultException e) {
            return null;
        }
    }

    @Override
    public OAuthToken getOAuthToken(String refreshToken, long expirationInterval) {
        try {
            Object result = em.createNamedQuery("OAuthToken.getOAuthToken")
                    .setParameter("refreshToken", refreshToken)
                    .setParameter("expirationInterval", expirationInterval)
                    .getSingleResult();
            return (OAuthToken) ((Object[]) result)[0];
        } catch (NoResultException e) {
            return null;
        }
    }
}
