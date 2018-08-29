package biocode.fims.rest.services.subResources;

import biocode.fims.application.config.FimsProperties;
import biocode.fims.config.network.NetworkConfig;
import biocode.fims.fimsExceptions.FimsRuntimeException;
import biocode.fims.fimsExceptions.ForbiddenRequestException;
import biocode.fims.fimsExceptions.errorCodes.ConfigCode;
import biocode.fims.models.Network;
import biocode.fims.rest.Compress;
import biocode.fims.rest.FimsController;
import biocode.fims.rest.filters.Admin;
import biocode.fims.rest.filters.Authenticated;
import biocode.fims.service.NetworkService;
import com.fasterxml.jackson.annotation.JsonProperty;
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
public class NetworkConfigurationResource extends FimsController {

    private final NetworkService networkService;

    @Autowired
    public NetworkConfigurationResource(NetworkService networkService, FimsProperties props) {
        super(props);
        this.networkService = networkService;
    }

    /**
     * Get a network config
     *
     * @param networkId
     * @return
     */
    @Compress
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public NetworkConfig getConfig(@PathParam("networkId") Integer networkId) {

        Network network = networkService.getNetwork(networkId);

        if (network == null) {
            throw new BadRequestException("Invalid networkId");
        }

        return network.getNetworkConfig();
    }

    /**
     * Update the network config
     *
     * @param config    The updated network object
     * @param networkId The id of the network to update
     */
    @PUT
    @Authenticated
    @Admin
    @Consumes(MediaType.APPLICATION_JSON)
    public Response update(@PathParam("networkId") Integer networkId,
                           NetworkConfig config) {

        if (!networkService.isNetworkAdmin(userContext.getUser(), networkId)) {
            throw new ForbiddenRequestException("You must be this network's admin in order to update the metadata");
        }

        try {
            networkService.saveConfig(config, networkId);
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
