package biocode.fims.rest.services.subResources;

import biocode.fims.config.project.ProjectConfig;
import biocode.fims.fimsExceptions.FimsRuntimeException;
import biocode.fims.fimsExceptions.ForbiddenRequestException;
import biocode.fims.fimsExceptions.errorCodes.ConfigCode;
import biocode.fims.fimsExceptions.errorCodes.GenericErrorCode;
import biocode.fims.models.Project;
import biocode.fims.rest.FimsController;
import biocode.fims.rest.Compress;
import biocode.fims.rest.filters.Admin;
import biocode.fims.rest.filters.Authenticated;
import biocode.fims.service.ProjectService;
import com.fasterxml.jackson.annotation.JsonProperty;
import biocode.fims.application.config.FimsProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.List;

/**
 * @author RJ Ewing
 */
@Controller
@Produces(MediaType.APPLICATION_JSON)
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
