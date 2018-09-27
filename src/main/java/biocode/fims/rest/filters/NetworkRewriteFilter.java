package biocode.fims.rest.filters;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.PreMatching;
import javax.ws.rs.core.UriInfo;
import java.io.IOException;
import java.util.Map;

/**
 * Filter which checks if the requestUri matches a "networkPath" with the networkId PathParm removed.
 * If a match is found, we rewrite the url to the mapped path so that jersey will find the correct
 * resource. This allows endpoints such as /networks/{networkId}/config to be accessible via /networks/config
 * for single network deployments.
 */
@PreMatching
public class NetworkRewriteFilter implements ContainerRequestFilter {

    private final Map<String, String> networkPaths;

    public NetworkRewriteFilter(Map<String, String> networkPaths) {
        this.networkPaths = networkPaths;
    }

    @Override
    public void filter(ContainerRequestContext requestContext)
            throws IOException {
        UriInfo uriInfo = requestContext.getUriInfo();
        String path = uriInfo.getPath();

        if (path.startsWith("/")) path = path.substring(1);
        if (path.endsWith("/")) path = path.substring(0, path.length() - 1);

        String mappedPath = networkPaths.get(path);
        if (mappedPath != null) {
            requestContext.setRequestUri(
                    uriInfo.getBaseUriBuilder().path(mappedPath).replaceQuery(uriInfo.getAbsolutePath().getRawQuery()).build()
            );
        }
    }
}
