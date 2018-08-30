package biocode.fims.repositories;

import biocode.fims.config.project.models.PersistedProjectConfig;
import biocode.fims.models.Project;
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
        return em.createQuery("SELECT DISTINCT p FROM Project AS p WHERE p.projectId = :id", Project.class)
                .setParameter("id", projectId)
                .setHint("javax.persistence.fetchgraph", em.getEntityGraph(entityGraph))
                .getSingleResult();
    }

    @Override
    public List<Project> getAll(List<Integer> projectIds, String entityGraph) {
        return em.createQuery("SELECT DISTINCT p FROM Project AS p WHERE p.projectId in :ids", Project.class)
                .setParameter("ids", projectIds)
                .setHint("javax.persistence.fetchgraph", em.getEntityGraph(entityGraph))
                .getResultList();
    }

    @Override
    public PersistedProjectConfig getConfig(int projectId) {
        // this is here b/c returning a single column from spring data repository using @Query requires the returning
        // type to be an interface
        return (PersistedProjectConfig) em.createQuery("SELECT persistedProjectConfig FROM Project WHERE projectId = :id", Object.class)
                .setParameter("id", projectId)
                .getSingleResult();
    }

}
