package biocode.fims.authorizers;

import biocode.fims.models.Project;
import biocode.fims.models.User;
import biocode.fims.repositories.ProjectRepository;

/**
 * @author rjewing
 */
public class ProjectAuthorizer {

    private final ProjectRepository projectRepository;

    public ProjectAuthorizer(ProjectRepository projectRepository) {
        this.projectRepository = projectRepository;
    }

    /**
     * check that a user can access the project's expeditions, data, etc. and that the project belongs to this
     * FIMS instance
     *
     * @param user
     * @param project
     * @return
     */
    public boolean userHasAccess(User user, Project project) {
        if (project == null) {
            return false;
        }
        if (user == null) {
            return project.isPublic();
        }

        return project.isPublic() || checkUserIsProjectMember(user, project.getProjectId());
    }

    private boolean checkUserIsProjectMember(User user, int projectId) {
        return user != null && projectRepository.userIsMember(projectId, user.getUserId());
    }

    /**
     * @param user
     * @param projectId
     * @return
     * @see ProjectAuthorizer#userHasAccess(User, Project)
     */
    public boolean userHasAccess(User user, int projectId) {
        Project project = projectRepository.getProjectByProjectId(projectId, null);

        return userHasAccess(user, project);
    }

}
