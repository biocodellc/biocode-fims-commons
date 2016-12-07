package biocode.fims.authorizers;

import biocode.fims.entities.Expedition;
import biocode.fims.entities.Project;
import biocode.fims.entities.User;
import biocode.fims.fimsExceptions.FimsRuntimeException;
import biocode.fims.fimsExceptions.errorCodes.ProjectCode;
import biocode.fims.service.ProjectService;
import biocode.fims.settings.SettingsManager;
import org.springframework.util.Assert;

import java.util.ArrayList;
import java.util.List;

/**
 * @author RJ Ewing
 */
public class QueryAuthorizer {
    private final ProjectService projectService;
    private final SettingsManager settingsManager;

    public QueryAuthorizer(ProjectService projectService, SettingsManager settingsManager) {
        this.projectService = projectService;
        this.settingsManager = settingsManager;
    }

    /**
     * Checks that the {@param user} has access to the {@param projectIds} and {@param expeditionCodes}
     *
     * @param projectIds      must contain at least 1 projectId
     * @param expeditionCodes
     * @param user
     * @return
     */
    public boolean authorizedQuery(List<Integer> projectIds, List<String> expeditionCodes, User user) {

        // if we have 0 projectIds, then it is impossible to authorize a query, since expeditionCodes are only
        // unique within a project.
        if (projectIds.size() < 1) {
            throw new FimsRuntimeException(ProjectCode.INVALID_PROJECT_LIST, 500);
        }

        List<Project> projects = projectService.getProjectsWithExpeditionsAndMembers(settingsManager.retrieveValue("appRoot"));

        if (expeditionCodes.size() > 0) {
            return authorizedExpeditionAccess(projectIds, expeditionCodes, projects, user);
        } else {
            return authorizedProjectAccess(projectIds, projects, user);
        }
    }

    /**
     * checks that every projectId is a valid project at this fims instance and is either a public project or the
     * user has access
     *
     * @param projectIds
     * @param projects
     * @param user       the user we are check access for. Can be null
     * @return
     */
    private boolean authorizedProjectAccess(List<Integer> projectIds, List<Project> projects, User user) {

        for (int projectId : projectIds) {
            boolean foundProject = false;

            for (Project project : projects) {
                if (project.getProjectId() == projectId) {
                    foundProject = true;

                    if (!authorizedUserForProject(project, user))
                        return false;
                }
            }

            if (!foundProject) {
                throw new FimsRuntimeException(ProjectCode.INVALID_PROJECT, 400, String.valueOf(projectId));
            }
        }

        return true;
    }

    private boolean authorizedUserForProject(Project project, User user) {
        return project.getUser().equals(user) || project.getProjectMembers().contains(user);
    }

    /**
     * checks that every expeditionCode is a public expedition, or the user owns the expedition, and that each
     * expeditionCode exists
     *
     * @param projectIds      must not be empty
     * @param expeditionCodes
     * @param projects
     * @param user            the user we are authorizing. can be null
     * @return
     */
    private boolean authorizedExpeditionAccess(List<Integer> projectIds, List<String> expeditionCodes, List<Project> projects, User user) {
        Assert.notEmpty(projectIds);

        boolean allAuthorizedExpeditions = true;
        List<String> foundExpeditionCodes = new ArrayList<>();

        for (Project project : projects) {

            // if the project is in the projectIds list, then we need to check if there is an expedition in the expeditionCodes list
            if (projectIds.contains(project.getProjectId())) {

                for (Expedition expedition : project.getExpeditions()) {

                    if (expeditionCodes.contains(expedition.getExpeditionCode())) {

                        foundExpeditionCodes.add(expedition.getExpeditionCode());

                        if (!expedition.isPublic() && !expedition.getUser().equals(user)) {

                            allAuthorizedExpeditions = false;

                        }

                    }
                }

            }

        }

        return allAuthorizedExpeditions && foundExpeditionCodes.size() == expeditionCodes.size();
    }
}
