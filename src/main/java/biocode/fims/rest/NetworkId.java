package biocode.fims.rest;

import biocode.fims.application.config.NetworkProperties;
import org.glassfish.hk2.utilities.binding.AbstractBinder;
import org.glassfish.jersey.process.internal.RequestScoped;

import javax.ws.rs.PathParam;

/**
 * @author rjewing
 */
public class NetworkId {
    private Integer networkId;

    public NetworkId(NetworkProperties properties, @PathParam("networkId") Integer networkId) {
        this.networkId = networkId == null ? properties.networkId() : networkId;
    }

    public Integer get() {
        return networkId;
    }

    /**
     * Register NetworkId class to be injectable via Jersey's @Context
     */
    public static class Binder extends AbstractBinder {
        @Override
        protected void configure() {
            bind(NetworkId.class).to(NetworkId.class).in(RequestScoped.class);
        }

    }
}
