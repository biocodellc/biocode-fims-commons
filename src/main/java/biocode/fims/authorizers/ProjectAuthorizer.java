package biocode.fims.authorizers;

import biocode.fims.entities.Project;
import biocode.fims.entities.User;
import biocode.fims.service.ProjectService;
import org.apache.commons.lang.StringUtils;

/**
 * @author rjewing
 */
public class ProjectAuthorizer {

    private final ProjectService projectService;
    private final String appRoot;

    public ProjectAuthorizer(ProjectService projectService, String appRoot) {
        this.projectService = projectService;
        this.appRoot = appRoot;
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
        if (project == null || !StringUtils.equals(project.getProjectUrl(), appRoot)) {
            return false;
        }
        if (user == null) {
            return project.isPublic();
        }

        return project.isPublic() || projectService.isUserMemberOfProject(user, project.getProjectId());
    }

    /**
     * @param user
     * @param projectId
     * @return
     * @see ProjectAuthorizer#userHasAccess(User, Project)
     */
    public boolean userHasAccess(User user, int projectId) {
        Project project = projectService.getProject(projectId);

        return userHasAccess(user, project);
    }

}
