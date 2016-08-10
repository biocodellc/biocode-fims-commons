package biocode.fims.repositories;

import biocode.fims.entities.Project;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

/**
 * Implementation of ProjectCustomOperations
 */
public class ProjectRepositoryImpl implements ProjectCustomOperations {
    @PersistenceContext(unitName = "entityManagerFactory")
    private EntityManager em;

    @Override
    public Project readByProjectId(int projectId, String entityGraph) {
        return em.createQuery("SELECT p FROM Project AS p WHERE p.projectId = :id", Project.class)
                .setParameter("id", projectId)
                .setHint("javax.persistence.fetchgraph", em.getEntityGraph(entityGraph))
                .getSingleResult();
    }

}
