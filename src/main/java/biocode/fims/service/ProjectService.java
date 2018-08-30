package biocode.fims.service;

import biocode.fims.config.network.NetworkConfig;
import biocode.fims.config.models.Entity;
import biocode.fims.config.project.ProjectConfig;
import biocode.fims.config.project.ProjectConfigUpdator;
import biocode.fims.config.project.models.PersistedProjectConfig;
import biocode.fims.fimsExceptions.FimsRuntimeException;
import biocode.fims.fimsExceptions.errorCodes.ConfigCode;
import biocode.fims.fimsExceptions.errorCodes.GenericErrorCode;
import biocode.fims.models.EntityIdentifier;
import biocode.fims.models.Expedition;
import biocode.fims.models.Project;
import biocode.fims.models.User;
import biocode.fims.repositories.ProjectRepository;
import biocode.fims.repositories.SetFimsUser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.PersistenceUnitUtil;
import java.util.ArrayList;
import java.util.List;


/**
 * Service class for handling {@link Project} persistence
 */
@Service
@Transactional
public class ProjectService {

    private final ExpeditionService expeditionService;
    @PersistenceContext(unitName = "entityManagerFactory")
    private EntityManager entityManager;

    private final ProjectRepository projectRepository;
    private final UserService userService;

    @Autowired
    public ProjectService(ProjectRepository projectRepository, ExpeditionService expeditionService,
                          UserService userService) {
        this.expeditionService = expeditionService;
        this.projectRepository = projectRepository;
        this.userService = userService;
    }

    public void create(Project project, int userId) {
        User user = entityManager.getReference(User.class, userId);
        project.setUser(user);

        update(project);
    }

    @SetFimsUser
    public void update(Project project) {
        validateAndSetProjectConfig(project);
        projectRepository.save(project);
    }

    public Project getProject(int projectId) {
        return projectRepository.findByProjectId(projectId);
    }


    @Transactional(readOnly = true)
    public boolean isUserMemberOfProject(User user, int projectId) {
        if (user == null) return false;

        PersistenceUnitUtil unitUtil = entityManager.getEntityManagerFactory().getPersistenceUnitUtil();
        if (!unitUtil.isLoaded(user, "projectsMemberOf")) {
            user = userService.getUserWithMemberProjects(user.getUsername());
        }

        for (Project userProject : user.getProjectsMemberOf()) {
            if (userProject.getProjectId() == projectId) {
                return true;
            }
        }

        return false;
    }

    public Project getProjectWithMembers(int projectId) {
        return projectRepository.getProjectByProjectId(projectId, "Project.withMembers");
    }

    public Project getProjectWithExpeditions(int projectId) {
        return projectRepository.getProjectByProjectId(projectId, "Project.withExpeditions");
    }

    public Project getProjectWithTemplates(int projectId) {
        return projectRepository.getProjectByProjectId(projectId, "Project.withTemplates");
    }

    /**
     * checks if a user is the admin of a specific project
     *
     * @param user
     * @param project
     * @return
     */
    public boolean isProjectAdmin(User user, Project project) {
        return project.getUser().equals(user);

    }

    /**
     * checks if a user is the admin of a specific project
     *
     * @param user
     * @param projectId
     * @return
     */
    public boolean isProjectAdmin(User user, int projectId) {
        if (user == null) {
            return false;
        }
        PersistenceUnitUtil unitUtil = entityManager.getEntityManagerFactory().getPersistenceUnitUtil();
        if (!unitUtil.isLoaded(user, "projects")) {
            // TODO maybe fetch user using entityGraph here?
            user = entityManager.merge(user);
        }

        for (Project p : user.getProjects()) {
            if (p.getProjectId() == projectId) {
                return true;
            }
        }
        return false;

    }

    public List<Project> getProjects() {
        return projectRepository.findAll();
    }

    public List<Project> getProjectsWithExpeditions(List<Integer> projectIds) {
        return projectRepository.getAll(projectIds, "Project.withExpeditions");
    }

    /**
     * get a list of projects which are public, or the user is a member of
     *
     * @param user
     * @param inludePublic
     * @return
     */
    public List<Project> getProjects(User user, boolean inludePublic) {
        List<Project> projects = projectRepository.findAll();
        List<Project> filteredProjects = new ArrayList<>();

        for (Project project : projects) {
            if ((inludePublic && project.isPublic()) ||
                    isUserMemberOfProject(user, project.getProjectId())) {
                filteredProjects.add(project);
            }
        }

        return filteredProjects;
    }

    private void validateAndSetProjectConfig(Project project) {
        if (project == null) {
            throw new FimsRuntimeException(GenericErrorCode.BAD_REQUEST, 400);
        }

        if (!project.hasConfigChanged()) {
            return;
        }

        ProjectConfig config = project.getProjectConfig();

        NetworkConfig networkConfig = project.getNetwork().getNetworkConfig();

        if (!config.isValid(networkConfig)) {
            throw new FimsRuntimeException(ConfigCode.INVALID, 400);
        }

        PersistedProjectConfig persistedProjectConfig = projectRepository.getConfig(project.getProjectId());
        ProjectConfig existingConfig = persistedProjectConfig.toProjectConfig(project.getNetwork().getNetworkConfig());

        if (existingConfig == null) {
            existingConfig = new ProjectConfig();
        }

        ProjectConfigUpdator updator = new ProjectConfigUpdator(config);
        project.setProjectConfig(updator.update(existingConfig));

        if (updator.newEntities().size() > 0) {
            createEntityBcids(updator.newEntities(), project.getProjectId());
        }

        // we don't delete records here
    }

    private void createEntityBcids(List<Entity> entities, int projectId) {
        for (Expedition e : expeditionService.getExpeditions(projectId, true)) {
            List<EntityIdentifier> entityIdentifiers = expeditionService.createEntityBcids(e, entities, e.getUser());
            e.getEntityIdentifiers().addAll(entityIdentifiers);
            expeditionService.update(e);
        }
    }
}


