package biocode.fims.service;

import biocode.fims.entities.Project;
import biocode.fims.entities.User;
import biocode.fims.repositories.ProjectRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.PersistenceUnitUtil;
import java.util.List;


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
        projectRepository.save(project);
    }

    public Project getProject(int projectId) {
        return projectRepository.findByProjectId(projectId);
    }


    @Transactional(readOnly = true)
    public boolean isUserMemberOfProject(User user, Project project) {
        boolean userIsProjectMember = false;

        PersistenceUnitUtil unitUtil = entityManager.getEntityManagerFactory().getPersistenceUnitUtil();
        if (!unitUtil.isLoaded(user, "projectsMemberOf")) {
            user = userService.getUserWithMemberProjects(user.getUsername());
        }

        for (Project userProject : user.getProjectsMemberOf()) {
            if (userProject.equals(project)) {
                userIsProjectMember = true;
                break;
            }
        }

        return userIsProjectMember;
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
}


