package biocode.fims.rest.services;

import biocode.fims.application.config.FimsProperties;
import biocode.fims.rest.FimsController;
import biocode.fims.rest.services.subResources.*;

import javax.ws.rs.Path;

/**
 * API endpoints for working with networks. This includes fetching details associated with networks.
 * Currently, there are no REST services for creating networks, which instead must be added to the Database
 * manually by an administrator
 *
 * @exclude
 */
public abstract class BaseNetworksController extends FimsController {

    BaseNetworksController(FimsProperties props) {
        super(props);
    }

    /**
     * @responseType biocode.fims.rest.services.subResources.NetworksResource
     */
    @Path("/")
    public Class<NetworksResource> getNetworksResource() {
        return NetworksResource.class;
    }

    /**
     * @responseType biocode.fims.rest.services.subResources.NetworkConfigurationResource
     * @resourceTag Config
     */
    @Path("/{networkId}/config")
    public Class<NetworkConfigurationResource> getNetworkConfigurationResource() {
        return NetworkConfigurationResource.class;
    }
}
