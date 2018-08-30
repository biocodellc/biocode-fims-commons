package biocode.fims.rest.services.subResources;

import biocode.fims.application.config.FimsProperties;
import biocode.fims.fimsExceptions.FimsRuntimeException;
import biocode.fims.fimsExceptions.ForbiddenRequestException;
import biocode.fims.models.Network;
import biocode.fims.rest.FimsController;
import biocode.fims.rest.UserEntityGraph;
import biocode.fims.rest.filters.Admin;
import biocode.fims.rest.filters.Authenticated;
import biocode.fims.serializers.Views;
import biocode.fims.service.NetworkService;
import biocode.fims.utils.Flag;
import com.fasterxml.jackson.annotation.JsonView;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.List;

/**
 * @author RJ Ewing
 */
@Controller
@Produces(MediaType.APPLICATION_JSON)
public class NetworksResource extends FimsController {
    private final NetworkService networkService;

    @Autowired
    public NetworksResource(NetworkService networkService,
                            FimsProperties props) {
        super(props);
        this.networkService = networkService;
    }

    /**
     * Fetch all available networks
     */
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
