package biocode.fims.rest.filters;

import biocode.fims.rest.versioning.APIVersion;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.PreMatching;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.UriInfo;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Filter which checks for the requested api version in the url. If a api version is found, then we remove the version
 * from the url and place it in the "api-version" header.
 */
@PreMatching
public class APIVersionFilter implements ContainerRequestFilter {
    @Context
    HttpServletRequest webRequest;

    private final static Pattern API_VERSION_PATTERN = Pattern.compile("v\\d\\.?\\d?");

    @Override
    public void filter(ContainerRequestContext requestContext)
            throws IOException {
        UriInfo uriInfo = requestContext.getUriInfo();
        String requestUri = String.valueOf(uriInfo.getRequestUri());

        Pattern urlAPIVersionPattern = Pattern.compile("^" + Pattern.quote(String.valueOf(uriInfo.getBaseUri())) +  API_VERSION_PATTERN + "\\/.+$");
        Matcher urlMatcher = urlAPIVersionPattern.matcher(requestUri);

        String version = APIVersion.DEFAULT_VERSION;
        try {
            if (urlMatcher.matches()) {
                Matcher versionMatcher = API_VERSION_PATTERN.matcher(requestUri);
                versionMatcher.find();

                version = versionMatcher.group();

                // remove the version from the requestUri
                requestContext.setRequestUri(new URI(requestUri.replaceFirst(version + "/", "")));
            }
        } catch (URISyntaxException e) {
            // do nothing
        }

        requestContext.getHeaders().add("Api-Version", version);
    }
}
