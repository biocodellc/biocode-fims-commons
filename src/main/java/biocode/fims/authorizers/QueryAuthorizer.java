package biocode.fims.authorizers;

import biocode.fims.fimsExceptions.errorCodes.ExpeditionCode;
import biocode.fims.models.Expedition;
import biocode.fims.models.Project;
import biocode.fims.models.User;
import biocode.fims.application.config.FimsProperties;
import biocode.fims.fimsExceptions.FimsRuntimeException;
import biocode.fims.fimsExceptions.errorCodes.ProjectCode;
import biocode.fims.service.ExpeditionService;
import biocode.fims.service.ProjectService;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.Assert;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author RJ Ewing
 */
public class QueryAuthorizer {
    private final static Logger logger = LoggerFactory.getLogger(QueryAuthorizer.class);

    private final ProjectService projectService;
    private final ExpeditionService expeditionService;
    private final FimsProperties props;

    public QueryAuthorizer(ProjectService projectService, ExpeditionService expeditionService, FimsProperties props) {
        this.projectService = projectService;
        this.expeditionService = expeditionService;
        this.props = props;
    }

    /**
     * Checks that the {@param user} has access to the {@param expeditionId}
     *
     * @param expeditionId
     * @param user
     * @return
     */
    public boolean authorizedQuery(int expeditionId, User user) {
        Expedition expedition = expeditionService.getExpedition(expeditionId);

        if (expedition == null) {
            throw new FimsRuntimeException(ExpeditionCode.INVALID_ID, 500);
        }

        // hack so expeditions list has expedition we're interested in
        expedition.getProject().setExpeditions(Collections.singletonList(expedition));

        return authorizedExpeditionAccess(
                Collections.singletonList(expedition.getProject().getProjectId()),
                Collections.singletonList(expedition.getExpeditionCode()),
                Collections.singletonList(expedition.getProject()),
                user
        );
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

        List<Project> projects = projectService.getProjectsWithExpeditions(projectIds);

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
        if (project.isPublic()) return true;
        if (user == null) {
            return project.isPublic();
        }
        return projectService.isUserMemberOfProject(user, project.getProjectId());
    }

    /**
     * checks that every expeditionCode is a public expedition, or the user is a member of the project the expedition
     * belongs to, and that each expeditionCode exists
     *
     * @param projectIds      must not be empty
     * @param expeditionCodes
     * @param projects
     * @param user            the user we are authorizing. can be null
     * @return
     */
    private boolean authorizedExpeditionAccess(List<Integer> projectIds, List<String> expeditionCodes, List<Project> projects, User user) {
        Assert.notEmpty(projectIds);

        List<String> foundExpeditionCodes = new ArrayList<>();

        for (Project project : projects) {

            // if the project is in the projectIds list, then we need to check if there is an expedition in the expeditionCodes list
            if (projectIds.contains(project.getProjectId())) {

                boolean userMemberOfProject = authorizedUserForProject(project, user);
                boolean foundExpeditionForProject = false;

                for (Expedition expedition : project.getExpeditions()) {

                    if (expeditionCodes.contains(expedition.getExpeditionCode())) {

                        foundExpeditionForProject = true;
                        foundExpeditionCodes.add(expedition.getExpeditionCode());

                    }
                }

                // since we are filtering on expeditionCodes, we only care about project access if we found an
                // expedition in the project
                if (foundExpeditionForProject && !project.isPublic() && !userMemberOfProject) {
                    return false;
                }

            }

        }

        // check that we found all expeditions to be in a Project in the projectIds list
        return foundExpeditionCodes.size() == expeditionCodes.size();
    }

    /**
     * Checks that the {@param user} has access to the {@param projectIds} and expedition.expeditionCode within the
     * {@param esQueryNode}
     *
     * @param projectIds
     * @param esQueryNode
     * @param user
     * @return
     */
    public boolean authorizedQuery(List<Integer> projectIds, ObjectNode esQueryNode, User user) {
        JsonParser parser = esQueryNode.traverse();
        List<String> expeditionCodes = new ArrayList<>();

        JsonToken token;
        try {
            while ((token = parser.nextToken()) != null) {

                if (token.equals(JsonToken.FIELD_NAME)) {
                    if (parser.getCurrentName().equals("expedition.expeditionCode")) {

                        if (parser.nextToken() == JsonToken.START_OBJECT) {

                            while ((token = parser.nextToken()) != JsonToken.END_OBJECT) {

                                if (token.equals(JsonToken.FIELD_NAME) && parser.getCurrentName().equals("query")) {
                                    parser.nextToken();
                                    expeditionCodes.add(parser.getValueAsString());
                                    break;
                                }
                            }
                        } else {
                            expeditionCodes.add(parser.getValueAsString());
                        }
                    }
                }
            }
        } catch (IOException e) {
            logger.warn("IOException parsing esQueryNode in QueryAuthorizer");
            return false;
        }

        return authorizedQuery(projectIds, expeditionCodes, user);
    }
}
