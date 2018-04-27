package biocode.fims.service;

import biocode.fims.digester.Entity;
import biocode.fims.fimsExceptions.FimsRuntimeException;
import biocode.fims.fimsExceptions.errorCodes.ConfigCode;
import biocode.fims.models.Expedition;
import biocode.fims.models.Project;
import biocode.fims.models.User;
import biocode.fims.projectConfig.ProjectConfig;
import biocode.fims.projectConfig.ProjectConfigUpdator;
import biocode.fims.repositories.ProjectConfigRepository;
import biocode.fims.repositories.ProjectRepository;
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
    private final ProjectConfigRepository projectConfigRepository;

    @Autowired
    public ProjectService(ProjectRepository projectRepository, ExpeditionService expeditionService,
                          UserService userService, ProjectConfigRepository projectConfigRepository) {
        this.expeditionService = expeditionService;
        this.projectRepository = projectRepository;
        this.userService = userService;
        this.projectConfigRepository = projectConfigRepository;
    }

    public void create(Project project, int userId) {
        User user = entityManager.getReference(User.class, userId);
        project.setUser(user);

        // save an empty config first as we can't validate the config until the projectId is generated
        ProjectConfig config = project.getProjectConfig();
        project.setProjectConfig(new ProjectConfig());

        // we need to save here so we can get the projectId
        projectRepository.save(project);

        project.setProjectConfig(config);

        createProjectSchema(project.getProjectId());
        saveConfig(config, project.getProjectId());
    }

    public void update(Project project) {
        saveConfig(project.getProjectConfig(), project.getProjectId());
        projectRepository.save(project);
    }

    public Project getProject(int projectId) {
        return projectRepository.findByProjectId(projectId);
    }

    public Project getProject(int projectId, String projectUrl) {
        return projectRepository.findByProjectIdAndProjectUrl(projectId, projectUrl);
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

    public Project getProjectWithTemplates(int projectId, String projectUrl) {
        Project project = projectRepository.getProjectByProjectId(projectId, "Project.withTemplates");

        if (project != null && project.getProjectUrl().equals(projectUrl)) {
            return project;
        }

        return null;
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

    public List<Project> getProjects(String projectUrl) {
        return projectRepository.findAllByProjectUrl(projectUrl);
    }

    public List<Project> getProjectsWithExpeditions(String projectUrl) {
        return projectRepository.getAllByProjectUrl(projectUrl, "Project.withExpeditions");
    }

    /**
     * get a list of projects for the current appRoot which are public, or the user is a member of
     *
     * @param appRoot
     * @param user
     * @param inludePublic
     * @return
     */
    public List<Project> getProjects(String appRoot, User user, boolean inludePublic) {
        List<Project> projects = projectRepository.findAllByProjectUrl(appRoot);
        List<Project> filteredProjects = new ArrayList<>();

        for (Project project : projects) {
            if ((inludePublic && project.isPublic()) ||
                    isUserMemberOfProject(user, project.getProjectId())) {
                filteredProjects.add(project);
            }
        }

        return filteredProjects;
    }

    public void createProjectSchema(int projectId) {
        projectConfigRepository.createProjectSchema(projectId);
    }

    public void saveConfig(ProjectConfig config, int projectId) {
        saveConfig(config, projectId, false);
    }

    /**
     * This is a temporary method to help with the migration to postgres. This will prevent creating bcids for entities
     * that are already registered
     */
    @Deprecated
    public void saveConfig(ProjectConfig config, int projectId, boolean checkForExistingBcids) {
        config.generateUris();

        if (!config.isValid()) {
            throw new FimsRuntimeException(ConfigCode.INVALID, 400);
        }

        ProjectConfig existingConfig = projectConfigRepository.getConfig(projectId);

        if (existingConfig == null) {
            existingConfig = new ProjectConfig();
        }

        ProjectConfigUpdator updator = new ProjectConfigUpdator(config);
        config = updator.update(existingConfig);

        if (updator.newEntities().size() > 0) {
            projectConfigRepository.createEntityTables(updator.newEntities(), projectId, config);
            createEntityBcids(updator.newEntities(), projectId, checkForExistingBcids);
        }

        if (updator.removedEntities().size() > 0) {
            projectConfigRepository.removeEntityTables(updator.removedEntities(), projectId);
        }

        projectConfigRepository.save(config, projectId);
    }

    private void createEntityBcids(List<Entity> entities, int projectId, boolean checkForExistingBcids) {
        for (Expedition e : expeditionService.getExpeditions(projectId, true)) {
            expeditionService.createEntityBcids(entities, e.getExpeditionId(), e.getUser(), checkForExistingBcids);
        }
    }
}


