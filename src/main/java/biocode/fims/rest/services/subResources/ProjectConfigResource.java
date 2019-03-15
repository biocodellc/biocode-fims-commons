package biocode.fims.rest.services.subResources;

import biocode.fims.application.config.FimsProperties;
import biocode.fims.config.project.ProjectConfig;
import biocode.fims.models.Project;
import biocode.fims.rest.Compress;
import biocode.fims.rest.FimsController;
import biocode.fims.service.ProjectService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

import javax.inject.Singleton;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.GET;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

/**
 * @author RJ Ewing
 */
@Controller
@Produces(MediaType.APPLICATION_JSON)
@Singleton
public class ProjectConfigResource extends FimsController {

    private final ProjectService projectService;

    @Autowired
    public ProjectConfigResource(ProjectService projectService, FimsProperties props) {
        super(props);
        this.projectService = projectService;
    }

    /**
     * Get a project config
     *
     * @param projectId
     * @return
     */
    @Compress
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public ProjectConfig getConfig(@PathParam("projectId") Integer projectId) {

        Project project = projectService.getProject(projectId);

        if (project == null) {
            throw new BadRequestException("Invalid projectId");
        }

        return project.getProjectConfig();
    }
}
