package biocode.fims.rest.services.rest.subResources;

import biocode.fims.entities.Project;
import biocode.fims.entities.User;
import biocode.fims.fimsExceptions.FimsRuntimeException;
import biocode.fims.fimsExceptions.ForbiddenRequestException;
import biocode.fims.rest.FimsService;
import biocode.fims.rest.UserEntityGraph;
import biocode.fims.rest.filters.Authenticated;
import biocode.fims.serializers.Views;
import biocode.fims.service.ProjectService;
import biocode.fims.service.UserService;
import biocode.fims.settings.SettingsManager;
import com.fasterxml.jackson.annotation.JsonView;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.List;

/**
 * @author RJ Ewing
 */
@Controller
@Produces(MediaType.APPLICATION_JSON)
public class ProjectMembersResource extends FimsService {
    private final ProjectService projectService;
    private final UserService userService;

    @Autowired
    public ProjectMembersResource(ProjectService projectService, UserService userService,
                                  SettingsManager settingsManager) {
        super(settingsManager);
        this.projectService = projectService;
        this.userService = userService;
    }

    /**
     * get project members
     *
     * @param projectId
     * @responseMessage 403 not the project's admin `biocode.fims.utils.ErrorInfo
     */
    @UserEntityGraph("User.withProjects")
    @JsonView(Views.Summary.class)
    @GET
    @Authenticated
    public List<User> getMembers(@PathParam("projectId") Integer projectId) {
        if (!projectService.isProjectAdmin(userContext.getUser(), projectId)) {
            throw new ForbiddenRequestException("You are not an admin to this project");
        }

        return projectService.getProjectWithMembers(projectId).getProjectMembers();
    }

    /**
     * remove a member user
     *
     * @param projectId the project to remove the user from
     * @param username  the username of the user to remove
     * @responseMessage 204 No Content
     * @responseMessage 403 not the project's admin `biocode.fims.utils.ErrorInfo
     * @responseMessage 404 user not found as project member `biocode.fims.utils.ErrorInfo
     */
    @UserEntityGraph("User.withProjects")
    @Path("{username}")
    @DELETE
    @Authenticated
    public Response removeMember(@PathParam("projectId") Integer projectId,
                                 @PathParam("username") String username) {
        if (!projectService.isProjectAdmin(userContext.getUser(), projectId)) {
            throw new ForbiddenRequestException("You are not an admin to this project");
        }

        User user = userService.getUserWithMemberProjects(username);

        if (user == null || !projectService.isUserMemberOfProject(user, projectId)) {
            throw new FimsRuntimeException("user not found as project member", 404);
        }

        Project project = projectService.getProjectWithMembers(projectId);

        if (project != null) {
            project.getProjectMembers().remove(user);
            user.getProjectsMemberOf().remove(project);
            // not sure why, but this only works by updating the user, not the project. Need to investigate the possibility
            // of a hibernate bug. A bi-directional relationship should be able to be updated from either side
            userService.update(user);
        }


        return Response.noContent().build();
    }

    /**
     * add a member user
     *
     * @param projectId the project to remove the user from
     * @param username  the username of the user to remove
     * @responseMessage 204 No Content
     * @responseMessage 403 not the project's admin `biocode.fims.utils.ErrorInfo
     * @responseMessage 404 user not found `biocode.fims.utils.ErrorInfo
     */
    @UserEntityGraph("User.withProjects")
    @Path("{username}")
    @PUT
    @Authenticated
    public Response addMember(@PathParam("projectId") Integer projectId,
                              @PathParam("username") String username) {
        if (!projectService.isProjectAdmin(userContext.getUser(), projectId)) {
            throw new ForbiddenRequestException("You are not an admin to this project");
        }

        User user = userService.getUserWithMemberProjects(username);

        if (user == null) {
            throw new FimsRuntimeException("user not found", 404);
        }

        Project project = projectService.getProjectWithMembers(projectId);
        project.getProjectMembers().add(user);
        user.getProjectsMemberOf().add(project);
        userService.update(user);

        return Response.noContent().build();
    }
}
