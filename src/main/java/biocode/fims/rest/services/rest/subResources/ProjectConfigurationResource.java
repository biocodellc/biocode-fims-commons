package biocode.fims.rest.services.rest.subResources;

import biocode.fims.models.Project;
import biocode.fims.projectConfig.ProjectConfig;
import biocode.fims.rest.FimsService;
import biocode.fims.service.ProjectService;
import biocode.fims.settings.SettingsManager;
import org.glassfish.jersey.server.model.Resource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;

/**
 * @author RJ Ewing
 */
@Controller
@Produces(MediaType.APPLICATION_JSON)
public class ProjectConfigurationResource extends FimsService {

    private final ProjectService projectService;

    @Autowired
    public ProjectConfigurationResource(ProjectService projectService, SettingsManager settingsManager) {
        super(settingsManager);
        this.projectService = projectService;
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public ProjectConfig getConfig(@PathParam("projectId") Integer projectId) {

        Project project = projectService.getProject(projectId, appRoot);

        if (project == null) {
            throw new BadRequestException("Invalid projectId");
        }

        return project.getProjectConfig();
    }

    /**
     *
     * @responseType biocode.fims.rest.services.rest.subResources.ProjectConfigurationListResource
     */
    @Path("/lists")
    public Resource getProjectConfigurationListResource() {
        return Resource.from(ProjectConfigurationListResource.class);

    }
}
