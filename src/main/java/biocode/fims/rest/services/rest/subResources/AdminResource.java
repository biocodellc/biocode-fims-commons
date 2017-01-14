package biocode.fims.rest.services.rest.subResources;

import biocode.fims.rest.FimsService;
import biocode.fims.rest.filters.Admin;
import biocode.fims.rest.filters.AuthenticatedUserResource;
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
@Admin
@AuthenticatedUserResource
@Produces(MediaType.APPLICATION_JSON)
public class AdminResource extends FimsService {

    @Autowired
    public AdminResource(SettingsManager settingsManager) {
        super(settingsManager);
    }

    /**
     *
     * @responseType biocode.fims.rest.services.rest.subResources.AdminProjectResource
     * @resourceTag Projects
     */
    @Path("/projects")
    public Resource getAdminProjectResource() {
        return Resource.from(AdminProjectResource.class);
    }

}
