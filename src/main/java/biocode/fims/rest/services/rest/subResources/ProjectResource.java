package biocode.fims.rest.services.rest.subResources;

import biocode.fims.entities.Project;
import biocode.fims.serializers.Views;
import biocode.fims.fimsExceptions.*;
import biocode.fims.rest.FimsService;
import biocode.fims.rest.UserEntityGraph;
import biocode.fims.rest.filters.Admin;
import biocode.fims.rest.filters.Authenticated;
import biocode.fims.rest.versioning.APIVersion;
import biocode.fims.service.ProjectService;
import biocode.fims.settings.SettingsManager;
import com.fasterxml.jackson.annotation.JsonView;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

import javax.ws.rs.*;
import javax.ws.rs.container.ResourceContext;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.List;

/**
 * @author RJ Ewing
 */
@Controller
@Produces(MediaType.APPLICATION_JSON)
public class ProjectResource extends FimsService {
    private final ProjectService projectService;

    @Autowired
    public ProjectResource(ProjectService projectService,
                           SettingsManager settingsManager) {
        super(settingsManager);
        this.projectService = projectService;
    }

    @JsonView(Views.Detailed.class)
    @UserEntityGraph("User.withProjectsMemberOf")
    @GET
    public List<Project> listProjects() {
        return projectService.getProjects(appRoot, userContext.getUser());
    }

    /**
     * Provides backwards compatibility for v1 api
     */
    @UserEntityGraph("User.withProjectsMemberOf")
    @Deprecated
    @GET
    @Path("/list")
    public List<Project> fetchList(@QueryParam("includePublic") @DefaultValue("false") boolean includePublic,
                                   @Context ResourceContext resourceContext) {

        if (APIVersion.version(headers.getHeaderString("Api-Version")) != APIVersion.v1_0) {
            throw new NotFoundException();
        }

        if (includePublic) {
            return listProjects();
        } else {
            if (userContext.getUser() == null) {
                throw new UnauthorizedRequestException("You must be logged in if you don't want to include public projects");
            }
            return resourceContext.getResource(UserProjectResource.class).listProjects();
        }
    }

    /**
     * Update a {@link Project}
     *
     * @param project   The updated project object
     * @param projectId The id of the project to update
     * @responseType biocode.fims.entities.Project
     * @responseMessage 403 not the project's admin `biocode.fims.utils.ErrorInfo
     */
    @JsonView(Views.Detailed.class)
    @POST
    @Authenticated
    @Admin
    @Path("/{projectId}/")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response updateConfig(@PathParam("projectId") Integer projectId,
                                 Project project) {
        if (!projectService.isProjectAdmin(userContext.getUser(), projectId)) {
            throw new ForbiddenRequestException("You must be this project's admin in order to update the metadata");
        }

        project.setProjectId(projectId);

        projectService.update(project);

        return Response.ok(project).build();

    }
}
