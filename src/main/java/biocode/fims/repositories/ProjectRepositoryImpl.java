package biocode.fims.repositories;

import biocode.fims.entities.Project;
import biocode.fims.repositories.customOperations.ProjectCustomOperations;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.util.List;

/**
 * Implementation of ProjectCustomOperations
 */
public class ProjectRepositoryImpl implements ProjectCustomOperations {
    @PersistenceContext(unitName = "entityManagerFactory")
    private EntityManager em;

    @Override
    public Project getProjectByProjectId(int projectId, String entityGraph) {
        return em.createQuery("SELECT DISTINCT p FROM Project AS p WHERE p.id = :id", Project.class)
                .setParameter("id", projectId)
                .setHint("javax.persistence.fetchgraph", em.getEntityGraph(entityGraph))
                .getSingleResult();
    }

    @Override
    public List<Project> getAllByProjectUrl(String projectUrl, String entityGraph) {
        return em.createQuery("SELECT DISTINCT p FROM Project AS p WHERE p.projectUrl= :url", Project.class)
                .setParameter("url", projectUrl)
                .setHint("javax.persistence.fetchgraph", em.getEntityGraph(entityGraph))
                .getResultList();
    }

}
