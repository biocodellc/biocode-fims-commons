package biocode.fims.rest;

import biocode.fims.rest.filters.NetworkRewriteFilter;

import javax.ws.rs.core.Feature;
import javax.ws.rs.core.FeatureContext;
import java.util.HashMap;
import java.util.Map;

/**
 * Jersey Feature for single network deployments
 */
public class SingleNetworkFeature implements Feature {

    private final int networkId;

    SingleNetworkFeature(int networkId) {
        this.networkId = networkId;
    }

    @Override
    public boolean configure(FeatureContext context) {
        final Map<String, String> networkPaths = new HashMap<>();

        context.register(new SingleNetworkPathRegister(networkId, networkPaths));
        context.register(new NetworkRewriteFilter(networkPaths));

        return true;
    }
}
