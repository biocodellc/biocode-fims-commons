package biocode.fims.repositories;

import biocode.fims.entities.OAuthToken;
import biocode.fims.entities.User;
import biocode.fims.repositories.customOperations.OAuthTokenCustomOperations;

import javax.persistence.*;
import javax.persistence.criteria.*;
import java.sql.Date;
import java.sql.Time;

/**
 * Implementation of OAuthTokenCustomOperations
 */
public class OAuthTokenRepositoryImpl implements OAuthTokenCustomOperations {
    @PersistenceContext(unitName = "entityManagerFactory")
    private EntityManager em;

    @Override
    public User getUser(String accessToken, long expirationInterval, String userEntityGraph) {
        CriteriaBuilder builder = em.getCriteriaBuilder();
        CriteriaQuery<User> cq = builder.createQuery(User.class);
        Root<OAuthToken> root = cq.from(OAuthToken.class);

        Join<OAuthToken, User> user = root.join("user", JoinType.INNER);

        if (userEntityGraph != null) {
            for (AttributeNode node : em.getEntityGraph(userEntityGraph).getAttributeNodes()) {

                String attributeName = node.getAttributeName();
                if (attributeName != null) {
                    user.fetch(attributeName, JoinType.LEFT);
                }

            }
        }

        Expression<Time> timeDiff = builder.function(
                "age",
                Time.class,
                builder.currentTimestamp(),
                root.<Date>get("created")
        );

        Expression<Integer> timeToSec = builder.function(
                "time_to_sec",
                Integer.class,
                timeDiff
        );

        Predicate hasToken = builder.equal(root.get("token"), accessToken);
        Predicate notExpired = builder.le(timeToSec, expirationInterval);
        cq.where(hasToken, notExpired);
        cq.select(user);

        try {
            return em.createQuery(cq).getSingleResult();
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
