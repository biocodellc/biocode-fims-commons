package biocode.fims.repositories.customOperations;

import biocode.fims.config.project.models.PersistedProjectConfig;
import org.springframework.data.repository.query.Param;

/**
 * defines custom ProjectConfigurationRepository operations
 */
public interface ProjectConfigurationCustomOperations {
    PersistedProjectConfig getConfig(@Param("id") int id);
}
