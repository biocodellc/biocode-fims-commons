package biocode.fims.rest.services.rest.subResources;

import biocode.fims.entities.Project;
import biocode.fims.rest.FimsService;
import biocode.fims.rest.UserEntityGraph;
import biocode.fims.rest.filters.AuthenticatedUserResource;
import biocode.fims.service.ProjectService;
import biocode.fims.settings.SettingsManager;
import org.glassfish.jersey.server.model.Resource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author RJ Ewing
 * @resourceTag Projects
 */
@Controller
@AuthenticatedUserResource
@Produces(MediaType.APPLICATION_JSON)
public class UserProjectResource extends FimsService {
    private final ProjectService projectService;

    @Autowired
    public UserProjectResource(ProjectService projectService, SettingsManager settingsManager) {
        super(settingsManager);
        this.projectService = projectService;
    }

    /**
     *
     * @responseType biocode.fims.rest.services.rest.subResources.UserProjectExpeditionsResource
     * @resourceTag Expeditions
     */
    @Path("{projectId}/expeditions")
    public Resource getUserProjectExpeditionsResource() {
        return Resource.from(UserProjectExpeditionsResource.class);
    }

    /**
     * Returns a list of projects that a user belongs to
     *
     * @return
     */
    @UserEntityGraph("User.withProjectsMemberOf")
    @GET
    public List<Project> listProjects() {
        return userContext.getUser().getProjectsMemberOf()
                .stream()
                .filter(p -> p.getProjectUrl().equals(appRoot))
                .collect(Collectors.toList());
    }
}
