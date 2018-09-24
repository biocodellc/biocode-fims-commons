package biocode.fims.rest;


import biocode.fims.rest.filters.NetworkRewriteFilter;
import com.fasterxml.classmate.ResolvedType;
import com.fasterxml.classmate.TypeResolver;
import org.glassfish.jersey.server.model.Resource;
import org.glassfish.jersey.server.model.ResourceMethod;
import org.glassfish.jersey.server.model.ResourceModel;
import org.glassfish.jersey.server.monitoring.ApplicationEvent;
import org.glassfish.jersey.server.monitoring.ApplicationEventListener;
import org.glassfish.jersey.server.monitoring.RequestEvent;
import org.glassfish.jersey.server.monitoring.RequestEventListener;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Registers a list of paths that contain a "networkId" PathParm.
 * Used in conjunction with {@link NetworkRewriteFilter} for single network deployments
 * <p>
 * note: there may be potential collisions between the mapped path and an existing path.
 * ex: network/{networkId} -> network collides w/ existing "network" path
 * This should not cause problems as the mapped class will take priority which is what we
 * most likely want. In the above example, we would return the single network instead of
 * the array of networks
 */
public class SingleNetworkPathRegister implements ApplicationEventListener {
    private static final Pattern pattern = Pattern.compile("\\{\\s*networkId\\s*(\\s*:.*)?}", Pattern.CASE_INSENSITIVE);
    private static final TypeResolver TYPE_RESOLVER = new TypeResolver();

    private final int networkId;
    private final Map<String, String> networkPaths;

    SingleNetworkPathRegister(int networkId, Map<String, String> networkPaths) {
        this.networkId = networkId;
        this.networkPaths = networkPaths;
    }

    @Override
    public void onEvent(ApplicationEvent event) {
        if (event.getType() == ApplicationEvent.Type.INITIALIZATION_APP_FINISHED) {
            final ResourceModel resourceModel = event.getResourceModel();

            Map<String, ResourceMethod> paths = new HashMap<>();
            resourceModel.getResources().forEach(resource -> paths.putAll(getPathsFromResource(resource)));

            for (Map.Entry<String, ResourceMethod> entry : paths.entrySet()) {
                String p = entry.getKey();
                Matcher matcher = pattern.matcher(p);
                if (matcher.find()) {
                    String key = matcher.replaceAll("").replace("//", "/");
                    if (key.startsWith("/")) key = key.substring(1);
                    if (key.endsWith("/")) key = key.substring(0, key.length() - 1);

                    networkPaths.put(
                            key,
                            matcher.replaceAll(String.valueOf(networkId))
                    );
                }
            }
        }
    }

    @Override
    public RequestEventListener onRequest(RequestEvent requestEvent) {
        return null;
    }

    private Map<String, ResourceMethod> getPathsFromResource(Resource resource) {
        return getPaths(null, false, resource);
    }

    private Map<String, ResourceMethod> getPaths(String basePath, Class<?> klass, boolean isLocator) {
        return getPaths(basePath, isLocator, Resource.from(klass));
    }

    private Map<String, ResourceMethod> getPaths(String basePath, boolean isLocator, Resource resource) {
        Map<String, ResourceMethod> paths = new HashMap<>();

        if (resource == null) return paths;

        if (!isLocator) {
            basePath = normalizePath(basePath, resource.getPath());
        }

        for (ResourceMethod method : resource.getResourceMethods()) {
            if (method.getHttpMethod().equalsIgnoreCase("OPTIONS") || basePath.contains(".wadl")) {
                continue;
            }
            paths.put(basePath, method);
        }

        for (Resource childResource : resource.getChildResources()) {
            for (ResourceMethod method : childResource.getAllMethods()) {
                if (method.getType() == ResourceMethod.JaxrsType.RESOURCE_METHOD) {
                    final String path = normalizePath(basePath, childResource.getPath());
                    if (method.getHttpMethod().equalsIgnoreCase("OPTIONS") || path.contains(".wadl")) {
                        continue;
                    }
                    paths.put(path, method);
                } else if (method.getType() == ResourceMethod.JaxrsType.SUB_RESOURCE_LOCATOR) {
                    final String path = normalizePath(basePath, childResource.getPath());
                    final ResolvedType responseType = TYPE_RESOLVER
                            .resolve(method.getInvocable().getResponseType());
                    final Class<?> erasedType = !responseType.getTypeBindings().isEmpty()
                            ? responseType.getTypeBindings().getBoundType(0).getErasedType()
                            : responseType.getErasedType();

                    paths.putAll(getPaths(path, erasedType, true));
                }
            }
        }

        if (resource.getResourceLocator() != null) {
            final ResolvedType responseType = TYPE_RESOLVER
                    .resolve(resource.getResourceLocator().getInvocable().getResponseType());
            final Class<?> erasedType = !responseType.getTypeBindings().isEmpty()
                    ? responseType.getTypeBindings().getBoundType(0).getErasedType()
                    : responseType.getErasedType();
            paths.putAll(getPaths(basePath, erasedType, true));
        }
        return paths;
    }

    private static String normalizePath(String basePath, String path) {
        if (basePath != null && basePath.startsWith("/")) {
            basePath = basePath.substring(1);
        }
        if (path == null) {
            return basePath == null ? "" : basePath;
        }
        if (basePath == null) {
            return path;
        }
        if (path.endsWith("/")) path = path.substring(0, path.length() - 1);
        if (basePath.endsWith("/")) {
            return path.startsWith("/") ? basePath + path.substring(1) : basePath + path;
        }
        return path.startsWith("/") ? basePath + path : basePath + "/" + path;
    }
}

