package biocode.fims.rest.services.subResources;

import biocode.fims.application.config.FimsProperties;
import biocode.fims.fimsExceptions.FimsRuntimeException;
import biocode.fims.fimsExceptions.ForbiddenRequestException;
import biocode.fims.fimsExceptions.errorCodes.ConfigCode;
import biocode.fims.models.ProjectConfiguration;
import biocode.fims.rest.Compress;
import biocode.fims.rest.FimsController;
import biocode.fims.rest.filters.Admin;
import biocode.fims.rest.filters.Authenticated;
import biocode.fims.serializers.JsonViewOverride;
import biocode.fims.serializers.Views;
import biocode.fims.service.ProjectConfigurationService;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonView;
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

    private final ProjectConfigurationService projectConfigurationService;

    @Autowired
    public ProjectConfigurationResource(ProjectConfigurationService projectConfigurationService, FimsProperties props) {
        super(props);
        this.projectConfigurationService = projectConfigurationService;
    }

    /**
     * Get all ProjectConfigurations
     */
    @JsonView(Views.Summary.class)
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public List<ProjectConfiguration> all() {
        return projectConfigurationService.getProjectConfigurations();
    }

    /**
     * Get a ProjectConfiguration
     *
     * @param id
     * @return
     */
    @JsonView(Views.DetailedConfig.class)
    @Compress
    @Path("{id}")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public ProjectConfiguration get(@PathParam("id") Integer id) {

        ProjectConfiguration configuration = projectConfigurationService.getProjectConfiguration(id);

        if (configuration == null) {
            throw new BadRequestException("Invalid config id");
        }

        return configuration;
    }

    /**
     * Update the ProjectConfiguration
     *
     * @param config The updated project configuration
     * @param id     The id of the configuration to update
     */
    @PUT
    @Path("{id}")
    @Authenticated
    @Consumes(MediaType.APPLICATION_JSON)
    public Response update(@PathParam("id") Integer id,
                           ProjectConfiguration config) {

        ProjectConfiguration configuration = projectConfigurationService.getProjectConfiguration(id);

        if (configuration == null) {
            throw new BadRequestException("Invalid config id");
        }

        if (!userContext.getUser().equals(configuration.getUser())) {
            throw new ForbiddenRequestException("You must be this configuration's admin in order to update");
        }

        try {
            projectConfigurationService.update(config);
        } catch (FimsRuntimeException e) {
            if (e.getErrorCode().equals(ConfigCode.INVALID)) {
                return Response.status(Response.Status.BAD_REQUEST).entity(new InvalidResponse(config.getProjectConfig().errors())).build();
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
