package biocode.fims.rest.services.rest.subResources;

import biocode.fims.rest.FimsService;
import biocode.fims.rest.filters.AuthenticatedUserResource;
import biocode.fims.service.ProjectService;
import biocode.fims.settings.SettingsManager;
import org.glassfish.jersey.server.model.Resource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

/**
 * @author RJ Ewing
 */
@Controller
@AuthenticatedUserResource
@Produces(MediaType.APPLICATION_JSON)
public class UserProjectResource extends FimsService {
    private final ProjectService projectService;

    @PathParam("userId")
    private int userId;

    @Autowired
    public UserProjectResource(ProjectService projectService, SettingsManager settingsManager) {
        super(settingsManager);
        this.projectService = projectService;
    }

    @Path("{projectId}/expeditions")
    public Resource getUserProjectExpeditionsResource() {
        return Resource.from(UserProjectExpeditionsResource.class);
    }

}
