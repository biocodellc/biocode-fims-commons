package biocode.fims.repositories;

import biocode.fims.models.OAuthNonce;
import biocode.fims.models.User;
import biocode.fims.models.UserInvite;
import biocode.fims.repositories.customOperations.OAuthNonceCustomOperations;
import biocode.fims.repositories.customOperations.UserInviteCustomOperations;

import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.PersistenceContext;
import java.util.UUID;

/**
 * Implementation of ProjectCustomOperations
 */
public class UserInviteRepositoryImpl implements UserInviteCustomOperations {
    @PersistenceContext(unitName = "entityManagerFactory")
    private EntityManager em;

    @Override
    public UserInvite getInvite(UUID id, long expirationInterval) {
        try {
            Object o = em.createNamedQuery("UserInvite.getInvite")
                    .setParameter("id", id)
                    .setParameter("expirationInterval", expirationInterval)
                    .getSingleResult();

            return (UserInvite) o;
        } catch (NoResultException e) {
            return null;
        }
    }
}
