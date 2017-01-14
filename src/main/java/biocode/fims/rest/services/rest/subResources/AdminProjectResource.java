package biocode.fims.rest.services.rest.subResources;

import biocode.fims.entities.Project;
import biocode.fims.rest.FimsService;
import biocode.fims.rest.UserEntityGraph;
import biocode.fims.rest.filters.Admin;
import biocode.fims.rest.filters.AuthenticatedUserResource;
import biocode.fims.service.ProjectService;
import biocode.fims.settings.SettingsManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

import javax.ws.rs.GET;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author RJ Ewing
 */
@Controller
@Admin
@AuthenticatedUserResource
@Produces(MediaType.APPLICATION_JSON)
public class AdminProjectResource extends FimsService {
    private final ProjectService projectService;

    @Autowired
    public AdminProjectResource(ProjectService projectService, SettingsManager settingsManager) {
        super(settingsManager);
        this.projectService = projectService;
    }

    /**
     * Get a list of projects where the user is an admin
     */
    @UserEntityGraph("User.withProjects")
    @GET
    public List<Project> listProjects() {
        return userContext.getUser().getProjects()
                .stream()
                .filter(p -> p.getProjectUrl().equals(appRoot))
                .collect(Collectors.toList());
    }
}
