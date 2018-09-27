package biocode.fims.service;

import biocode.fims.config.models.Entity;
import biocode.fims.fimsExceptions.BadRequestException;
import biocode.fims.fimsExceptions.DataIntegrityMessage;
import biocode.fims.models.*;
import biocode.fims.repositories.ProjectRepository;
import biocode.fims.repositories.SetFimsUser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.PersistenceUnitUtil;
import java.util.ArrayList;
import java.util.LinkedList;
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

    public Project create(Project project) {
        try {
            Project p = projectRepository.save(project);

            User user = loadMemberProjects(p.getUser());
            user.getProjectsMemberOf().add(p);
            userService.update(user);
            return p;
        } catch (DataIntegrityViolationException e) {
            throw new BadRequestException(new DataIntegrityMessage(e).toString(), e);
        }
    }

    public void update(Project project) {
        projectRepository.save(project);
    }

    public Project getProject(int projectId) {
        return projectRepository.findByProjectId(projectId);
    }


    @Transactional(readOnly = true)
    public boolean isUserMemberOfProject(User user, int projectId) {
        if (user == null) return false;

        user = loadMemberProjects(user);

        for (Project userProject : user.getProjectsMemberOf()) {
            if (userProject.getProjectId() == projectId) {
                return true;
            }
        }

        return false;
    }

    private User loadMemberProjects(User user) {
        PersistenceUnitUtil unitUtil = entityManager.getEntityManagerFactory().getPersistenceUnitUtil();
        if (!unitUtil.isLoaded(user, "projectsMemberOf")) {
            return userService.getUserWithMemberProjects(user.getUsername());
        }
        return user;
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

    void createEntityBcids(List<Entity> entities, int configId) {
        for (Project project : projectRepository.findAllByProjectConfigurationId(configId)) {
            for (Expedition e : expeditionService.getExpeditions(project.getProjectId(), true)) {
                List<EntityIdentifier> entityIdentifiers = expeditionService.createEntityBcids(e, entities, e.getUser());
                e.getEntityIdentifiers().addAll(entityIdentifiers);
                expeditionService.update(e);
            }
        }
    }
}


