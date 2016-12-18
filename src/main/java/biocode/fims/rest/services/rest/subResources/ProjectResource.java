package biocode.fims.rest.services.rest.subResources;

import biocode.fims.entities.Project;
import biocode.fims.fimsExceptions.UnauthorizedRequestException;
import biocode.fims.rest.FimsService;
import biocode.fims.rest.UserEntityGraph;
import biocode.fims.rest.versioning.APIVersion;
import biocode.fims.service.ProjectService;
import biocode.fims.settings.SettingsManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

import javax.ws.rs.*;
import javax.ws.rs.container.ResourceContext;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import java.util.List;

/**
 * @author RJ Ewing
 */
@Controller
@Produces(MediaType.APPLICATION_JSON)
public class ProjectResource extends FimsService {
    private final ProjectService projectService;

    @Context
    ResourceContext resourceContext;


    @Autowired
    public ProjectResource(ProjectService projectService,
                           SettingsManager settingsManager) {
        super(settingsManager);
        this.projectService = projectService;
    }

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
    public List<Project> fetchList(@QueryParam("includePublic") @DefaultValue("false") boolean includePublic) {

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
}
