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
public class ProjectConfigurationResource extends FimsController {

    private final ProjectService projectService;

    @Autowired
    public ProjectConfigurationResource(ProjectService projectService, FimsProperties props) {
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

    /**
     * Update the project config
     *
     * @param config    The updated project object
     * @param projectId The id of the project to update
     */
    @PUT
    @Authenticated
    @Admin
    @Consumes(MediaType.APPLICATION_JSON)
    public Response update(@PathParam("projectId") Integer projectId,
                           ProjectConfig config) {

        Project project = projectService.getProject(projectId);

        if (project == null) throw new FimsRuntimeException(GenericErrorCode.BAD_REQUEST, 400);

        if (!projectService.isProjectAdmin(userContext.getUser(), project)) {
            throw new ForbiddenRequestException("You must be this project's admin in order to update the metadata");
        }

        project.setProjectConfig(config);

        try {
            projectService.update(project);
        } catch (FimsRuntimeException e) {
            if (e.getErrorCode().equals(ConfigCode.INVALID)) {
                return Response.status(Response.Status.BAD_REQUEST).entity(new InvalidResponse(config.errors())).build();
            } else {
                throw e;
            }

        }

        return Response.ok(config).build();
    }

    private static class InvalidResponse {
        @JsonProperty
        private List<String> errors;

        InvalidResponse(List<String> errors) {
            this.errors = errors;
        }
    }
}
