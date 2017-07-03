package biocode.fims.rest.services.rest.subResources;

import biocode.fims.application.config.FimsProperties;
import biocode.fims.rest.FimsService;
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

    @Autowired
    public ProjectConfigurationResource(FimsProperties props) {
        super(props);
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
