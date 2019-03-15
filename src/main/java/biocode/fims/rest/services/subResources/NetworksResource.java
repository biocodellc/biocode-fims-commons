package biocode.fims.rest.services.subResources;

import biocode.fims.application.config.FimsProperties;
import biocode.fims.models.Network;
import biocode.fims.rest.Compress;
import biocode.fims.rest.FimsController;
import biocode.fims.rest.NetworkId;
import biocode.fims.rest.UserEntityGraph;
import biocode.fims.rest.filters.Admin;
import biocode.fims.rest.filters.Authenticated;
import biocode.fims.serializers.Views;
import biocode.fims.service.NetworkService;
import com.fasterxml.jackson.annotation.JsonView;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

import javax.inject.Singleton;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import java.util.List;

/**
 * @author RJ Ewing
 */
@Controller
@Produces(MediaType.APPLICATION_JSON)
@Singleton
public class NetworksResource extends FimsController {
    private final NetworkService networkService;

    @Context
    NetworkId networkId;

    @Autowired
    public NetworksResource(NetworkService networkService,
                            FimsProperties props) {
        super(props);
        this.networkService = networkService;
    }

    @Compress
    @JsonView(Views.DetailedConfig.class)
    @Path("{networkId}")
    @GET
    public Network getNetwork() {
        return networkService.getNetwork(networkId.get());
    }

    /**
     * Fetch all available networks
     */
    @Compress
    @JsonView(Views.Detailed.class)
    @GET
    public List<Network> getNetworks() {
        return networkService.getNetworks();
    }


    /**
     * Update a {@link Network}
     *
     * @param network   The updated network object
     * @param networkId The id of the network to update
     * @responseMessage 403 not the network's admin `biocode.fims.utils.ErrorInfo
     */
    @Compress
    @UserEntityGraph("User.withNetworks")
    @JsonView(Views.Detailed.class)
    @PUT
    @Authenticated
    @Admin
    @Path("/{networkId}/")
    @Consumes(MediaType.APPLICATION_JSON)
    public Network updateNetwork(@PathParam("networkId") Integer networkId,
                                  Network network) {
        network.setId(networkId);
        network.setUser(userContext.getUser());
        return networkService.update(network);
    }
}
