package biocode.fims.service;

import biocode.fims.entities.Expedition;
import biocode.fims.entities.Project;
import biocode.fims.entities.User;
import biocode.fims.repositories.ProjectRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.PersistenceUnitUtil;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;


/**
 * Service class for handling {@link Project} persistence
 */
@Service
@Transactional
public class ProjectService {

    @PersistenceContext(unitName = "entityManagerFactory")
    private EntityManager entityManager;

    private final ProjectRepository projectRepository;
    private final UserService userService;

    @Autowired
    public ProjectService(ProjectRepository projectRepository, UserService userService) {
        this.projectRepository = projectRepository;
        this.userService = userService;
    }

    public void create(Project project, int userId) {
        User user = entityManager.getReference(User.class, userId);
        project.setUser(user);
        projectRepository.save(project);

    }

    public void update(Project project) {
        Project p = getProject(project.getProjectId());
        project.setProjectUrl(p.getProjectUrl());
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
}


