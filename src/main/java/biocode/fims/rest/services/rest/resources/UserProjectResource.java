package biocode.fims.rest.services.rest.resources;

import biocode.fims.rest.FimsService;
import biocode.fims.rest.filters.Authenticated;
import biocode.fims.rest.filters.AuthenticatedUserResource;
import biocode.fims.service.ExpeditionService;
import biocode.fims.service.ProjectService;
import biocode.fims.settings.SettingsManager;
import org.glassfish.jersey.server.model.Resource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

import javax.ws.rs.Path;
import javax.ws.rs.PathParam;

/**
 * @author RJ Ewing
 */
@Controller
@AuthenticatedUserResource
public class UserProjectResource extends FimsService {
    private final ProjectService projectService;

//    @Autowired
//    UserProjectExpeditionsResource userProjectExpeditionsResource;

    @PathParam("userId")
    private int userId;

    @Autowired
    public UserProjectResource(ProjectService projectService, SettingsManager settingsManager) {
        super(settingsManager);
        this.projectService = projectService;
    }

    @Path("{projectId}/expeditions")
    public Resource getUserProjectExpeditionsResource(@PathParam("projectId") Integer projectId) {
        return Resource.from(UserProjectExpeditionsResource.class);
//        return userProjectExpeditionsResource;
    }


}
