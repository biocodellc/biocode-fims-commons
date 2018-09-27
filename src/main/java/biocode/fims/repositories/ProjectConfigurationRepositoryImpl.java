package biocode.fims.repositories;

import biocode.fims.config.project.models.PersistedProjectConfig;
import biocode.fims.repositories.customOperations.ProjectConfigurationCustomOperations;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

public class ProjectConfigurationRepositoryImpl implements ProjectConfigurationCustomOperations {
    @PersistenceContext(unitName = "entityManagerFactory")
    private EntityManager em;

    @Override
    public PersistedProjectConfig getConfig(int id) {
        // this is here b/c returning a single column from spring data repository using @Query requires the returning
        // type to be an interface
        return (PersistedProjectConfig) em.createQuery("SELECT persistedProjectConfig FROM ProjectConfiguration WHERE id = :id", Object.class)
                .setParameter("id", id)
                .getSingleResult();
    }

}
