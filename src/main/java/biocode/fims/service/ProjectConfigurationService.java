package biocode.fims.service;

import biocode.fims.config.network.NetworkConfig;
import biocode.fims.config.project.ProjectConfig;
import biocode.fims.config.project.ProjectConfigUpdator;
import biocode.fims.config.project.models.PersistedProjectConfig;
import biocode.fims.fimsExceptions.FimsRuntimeException;
import biocode.fims.fimsExceptions.errorCodes.ConfigCode;
import biocode.fims.fimsExceptions.errorCodes.GenericErrorCode;
import biocode.fims.models.*;
import biocode.fims.repositories.ProjectConfigurationRepository;
import biocode.fims.repositories.SetFimsUser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.util.List;


/**
 * Service class for handling {@link ProjectConfiguration} persistence
 */
@Service
@Transactional
public class ProjectConfigurationService {

    @PersistenceContext(unitName = "entityManagerFactory")
    private EntityManager entityManager;

    private final ProjectConfigurationRepository projectConfigurationRepository;
    private final ProjectService projectService;

    @Autowired
    public ProjectConfigurationService(ProjectConfigurationRepository projectConfigurationRepository, ProjectService projectService) {
        this.projectConfigurationRepository = projectConfigurationRepository;
        this.projectService = projectService;
    }

    @SetFimsUser
    public ProjectConfiguration create(ProjectConfiguration config, int userId) {
        User user = entityManager.getReference(User.class, userId);
        config.setUser(user);

        return update(config);
    }

    @SetFimsUser
    public ProjectConfiguration update(ProjectConfiguration config) {
        validateProjectConfig(config);
        return projectConfigurationRepository.save(config);
    }

    public ProjectConfiguration getProjectConfiguration(int id) {
        return projectConfigurationRepository.findById(id);
    }

    public List<ProjectConfiguration> getProjectConfigurations(User user) {
        return user == null
                ? projectConfigurationRepository.findAll()
                : projectConfigurationRepository.findAllByUserUserId(user.getUserId());
    }

    public List<ProjectConfiguration> getNetworkApprovedProjectConfigurations(User user) {
        return user == null
                ? projectConfigurationRepository.findAllByNetworkApproved(true)
                : projectConfigurationRepository.findAllByNetworkApprovedOrUserUserId(true, user.getUserId());
    }

    private void validateProjectConfig(ProjectConfiguration projectConfiguration) {
        if (projectConfiguration == null) {
            throw new FimsRuntimeException(GenericErrorCode.BAD_REQUEST, 400);
        }

        if (!projectConfiguration.hasConfigChanged()) {
            return;
        }

        ProjectConfig config = projectConfiguration.getProjectConfig();

        NetworkConfig networkConfig = projectConfiguration.getNetwork().getNetworkConfig();

        if (!config.isValid(networkConfig)) {
            throw new FimsRuntimeException(ConfigCode.INVALID, 400);
        }

        ProjectConfig existingConfig;
        try {
            PersistedProjectConfig persistedProjectConfig = projectConfigurationRepository.getConfig(projectConfiguration.getId());
            existingConfig = persistedProjectConfig.toProjectConfig(projectConfiguration.getNetwork().getNetworkConfig());
        } catch (EmptyResultDataAccessException e) {
            existingConfig = new ProjectConfig();
        }

        ProjectConfigUpdator updator = new ProjectConfigUpdator(config);
        projectConfiguration.setProjectConfig(updator.update(existingConfig));

        if (updator.newEntities().size() > 0) {
            projectService.createEntityBcids(updator.newEntities(), projectConfiguration.getId());
        }
    }
}


