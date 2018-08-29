package biocode.fims.rest.services;

import biocode.fims.application.config.FimsProperties;
import biocode.fims.rest.FimsController;
import biocode.fims.rest.services.subResources.*;
import biocode.fims.service.NetworkService;
import org.glassfish.jersey.server.model.Resource;

import javax.ws.rs.Path;

/**
 * API endpoints for working with networks. This includes fetching details associated with networks.
 * Currently, there are no REST services for creating networks, which instead must be added to the Database
 * manually by an administrator
 *
 * @exclude
 */
public abstract class BaseNetworksController extends FimsController {

    private final NetworkService networkService;

    BaseNetworksController(NetworkService networkService, FimsProperties props) {
        super(props);
        this.networkService = networkService;
    }


    /**
     * @responseType biocode.fims.rest.services.subResources.NetworksResource
     */
    @Path("/")
    public Resource getNetworksResource() {
        return Resource.from(NetworksResource.class);
    }

    /**
     * @responseType biocode.fims.rest.services.subResources.NetworkConfigurationResource
     * @resourceTag Config
     */
    @Path("/{networkId}/config")
    public Resource getNetworkConfigurationResource() {
        return Resource.from(NetworkConfigurationResource.class);
    }
}
