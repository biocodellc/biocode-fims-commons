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
import biocode.fims.utils.Flag;
import com.fasterxml.jackson.annotation.JsonView;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

import javax.ws.rs.*;
import javax.ws.rs.container.ResourceContext;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author RJ Ewing
 */
@Controller
@Produces(MediaType.APPLICATION_JSON)
public class ProjectsResource extends FimsService {
    private final ProjectService projectService;

    @Autowired
    public ProjectsResource(ProjectService projectService,
                            SettingsManager settingsManager) {
        super(settingsManager);
        this.projectService = projectService;
    }

    /**
     * Fetch all projects available to the current user
     *
     * @param includePublic If we should include public projects
     * @param admin         Flag used to request projects the authenticated user is an admin for. Note: this flag
     *                      takes precedence over all other query params
     */
    @JsonView(Views.Detailed.class)
    @UserEntityGraph("User.withProjectsAndProjectsMemberOf")
    @GET
    public List<Project> getProjects(@QueryParam("includePublic") @DefaultValue("true") Boolean includePublic,
                                     @QueryParam("admin") @DefaultValue("false") Flag admin) {
        if (admin.isPresent()) {
            return userContext.getUser().getProjects()
                    .stream()
                    .filter(p -> p.getProjectUrl().equals(appRoot))
                    .collect(Collectors.toList());
        }

        return projectService.getProjects(appRoot, userContext.getUser(), includePublic);
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

        if (!includePublic && userContext.getUser() == null) {
            throw new UnauthorizedRequestException("You must be logged in if you don't want to include public projects");
        }
        return getProjects(includePublic, new Flag(null));
    }

    /**
     * Update a {@link Project}
     *
     * @param project   The updated project object
     * @param projectId The id of the project to update
     * @responseType biocode.fims.entities.Project
     * @responseMessage 403 not the project's admin `biocode.fims.utils.ErrorInfo
     */
    @UserEntityGraph("User.withProjects")
    @JsonView(Views.Detailed.class)
    @POST
    @Authenticated
    @Admin
    @Path("/{projectId}/")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response updateProject(@PathParam("projectId") Integer projectId,
                                  Project project) {
        if (!projectService.isProjectAdmin(userContext.getUser(), projectId)) {
            throw new ForbiddenRequestException("You must be this project's admin in order to update the metadata");
        }

        Project existingProject = projectService.getProject(projectId);

        if (existingProject == null || !existingProject.getProjectUrl().equals(appRoot)) {
            throw new FimsRuntimeException("project not found", 404);
        }

        updateExistingProject(existingProject, project);
        projectService.update(existingProject);

        return Response.ok(existingProject).build();

    }

    /**
     * method to transfer the updated {@link Project} object to an existing {@link Project}. This
     * allows us to control which properties can be updated.
     * Currently allows updating of the following properties : projectAbstract, projectTitle, isPublic, and validationXml
     * @param existingProject
     * @param updatedProject
     */
    private void updateExistingProject(Project existingProject, Project updatedProject) {
        existingProject.setProjectTitle(updatedProject.getProjectTitle());
        existingProject.setProjectAbstract(updatedProject.getProjectAbstract());
        existingProject.setValidationXml(updatedProject.getValidationXml());
        existingProject.setPublic(updatedProject.isPublic());
    }
}
