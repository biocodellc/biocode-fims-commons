package biocode.fims.rest.services.subResources;

import biocode.fims.application.config.FimsProperties;
import biocode.fims.fimsExceptions.*;
import biocode.fims.fimsExceptions.BadRequestException;
import biocode.fims.fimsExceptions.errorCodes.ConfigCode;
import biocode.fims.models.ProjectConfiguration;
import biocode.fims.models.User;
import biocode.fims.rest.Compress;
import biocode.fims.rest.FimsController;
import biocode.fims.rest.filters.Authenticated;
import biocode.fims.rest.responses.InvalidConfigurationResponse;
import biocode.fims.serializers.Views;
import biocode.fims.service.ProjectConfigurationService;
import biocode.fims.utils.Flag;
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
    public List<ProjectConfiguration> all(@QueryParam("networkApproved") @DefaultValue("false") Flag networkApproved,
                                          @QueryParam("user") @DefaultValue("false") Flag includeUser) {
        if (includeUser.isPresent() && userContext.getUser() == null) {
            throw new BadRequestException("user flag must not be present for un-authenticated requests");
        }

        User user = includeUser.isPresent() ? userContext.getUser() : null;

        return networkApproved.isPresent()
                ? projectConfigurationService.getNetworkApprovedProjectConfigurations(user)
                : projectConfigurationService.getProjectConfigurations(user);
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

        updateExistingProjectConfiguration(configuration, config);

        try {
            projectConfigurationService.update(configuration);
        } catch (FimsRuntimeException e) {
            if (e.getErrorCode().equals(ConfigCode.INVALID)) {
                return Response.status(Response.Status.BAD_REQUEST).entity(new InvalidConfigurationResponse(config.getProjectConfig().errors())).build();
            } else {
                throw e;
            }

        }

        return Response.ok(config).build();
    }


    /**
     * method to transfer the updated {@link ProjectConfiguration} object to an existing {@link ProjectConfiguration}. This
     * allows us to control which properties can be updated.
     * Currently allows updating of the following properties : description, name, config
     */
    private void updateExistingProjectConfiguration(ProjectConfiguration existing, ProjectConfiguration updated) {
        existing.setName(updated.getName());
        existing.setDescription(updated.getDescription());
        existing.setProjectConfig(updated.getProjectConfig());
    }
}
