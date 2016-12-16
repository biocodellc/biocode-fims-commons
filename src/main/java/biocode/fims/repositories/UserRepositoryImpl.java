package biocode.fims.repositories;

import biocode.fims.entities.User;
import biocode.fims.repositories.customOperations.UserCustomOperations;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

/**
 * Implementation of UserCustomOperations
 * @author RJ Ewing
 */
public class UserRepositoryImpl implements UserCustomOperations {
    @PersistenceContext(unitName = "entityManagerFactory")
    private EntityManager em;

    @Override
    public User getUser(int userId, String entityGraph) {
        return em.createQuery("SELECT DISTINCT u FROM User u WHERE u.userId=:userId", User.class)
                .setParameter("userId", userId)
                .setHint("javax.persistence.fetchgraph", em.getEntityGraph(entityGraph))
                .getSingleResult();
    }
}
