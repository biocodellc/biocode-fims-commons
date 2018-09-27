package biocode.fims.rest.filters;

import biocode.fims.rest.versioning.APIVersion;
import biocode.fims.rest.versioning.VersionUrlConfig;
import org.apache.commons.lang3.text.StrSubstitutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.PreMatching;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Filter which checks for the requested api version in the url. If a api version is found, then we remove the version
 * from the url and place it in the "api-version" header.
 * <p>
 * Then we will check to see if the resource has moved. The {@link VersionUrlConfig} is parsed from the api-version-urls.yaml file.
 * If we use each {@link VersionUrlConfig.VersionUrlData#getVersionUrl()} regex to test against the incoming path.
 * If we find a match, we will set requestContext.requestUri using {@link VersionUrlConfig.VersionUrlData#getCurrentUrl()},
 * performing string substitution using and {@link VersionUrlConfig.VersionUrlData#getNamedGroups()} found in the
 * VersionUrlData.versionUrl. We will then set any defaultQueryParams froun in {@link VersionUrlConfig.VersionUrlData#getDefaultQueryParams()}.
 * These defaultQueryParams will be overridden by any incoming queryParams.
 */
@PreMatching
public class APIVersionFilter implements ContainerRequestFilter {
    private static final Logger logger = LoggerFactory.getLogger(APIVersionFilter.class);

    @Context
    HttpServletRequest webRequest;
    @Autowired
    private VersionUrlConfig versionUrlConfig;

    private final static Pattern API_VERSION_PATTERN = Pattern.compile("v\\d\\.?\\d?");

    @Override
    public void filter(ContainerRequestContext requestContext)
            throws IOException {
        UriInfo uriInfo = requestContext.getUriInfo();
        String requestUri = String.valueOf(uriInfo.getRequestUri());

        // check if the version is in the url
        Pattern urlAPIVersionPattern = Pattern.compile("^" + Pattern.quote(String.valueOf(uriInfo.getBaseUri())) + API_VERSION_PATTERN + "\\/.+$");
        Matcher urlMatcher = urlAPIVersionPattern.matcher(requestUri);

        String versionString = APIVersion.DEFAULT_VERSION;

        if (urlMatcher.matches()) {
            Matcher versionMatcher = API_VERSION_PATTERN.matcher(requestUri);
            versionMatcher.find();

            versionString = versionMatcher.group();

            // remove the version from the requestUri
            try {
                requestContext.setRequestUri(new URI(requestUri.replaceFirst(versionString + "/", "")));
                requestUri = String.valueOf(uriInfo.getRequestUri());
            } catch (URISyntaxException e) {
                logger.warn("error creating URI from string {}", requestUri.replaceFirst(versionString + "/", ""));
            }
        }

        // update the requestUri if the resource has moved locations
        updateRequestUriPath(requestContext, APIVersion.version(versionString), uriInfo);

        requestContext.getHeaders().add("Api-Version", versionString);
    }

    private void updateRequestUriPath(ContainerRequestContext requestContext, APIVersion version, UriInfo requestUriInfo) {
        VersionUrlConfig.VersionUrlData currentPathData = getCurrentPathData(version, requestUriInfo.getPath());

        if (currentPathData != null) {
            try {
                UriBuilder builder = requestUriInfo.getBaseUriBuilder();
                StrSubstitutor sub = new StrSubstitutor(currentPathData.getNamedGroupMap());

                // add the new path
                builder.path(sub.replace(currentPathData.getCurrentUrl()));

                // add any default queryParams first
                for (Map.Entry<String, String> pEntry : currentPathData.getDefaultQueryParams().entrySet()) {
                    builder.replaceQueryParam(pEntry.getKey(), pEntry.getValue());
                }

                // add any request queryParams as these will override the defaults
                for (Map.Entry<String, List<String>> pEntry : requestUriInfo.getQueryParameters().entrySet()) {
                    if (pEntry.getValue().size() == 1) {
                        builder.replaceQueryParam(pEntry.getKey(), pEntry.getValue().get(0));
                    } else {
                        for (String val: pEntry.getValue()) {
                            builder.queryParam(pEntry.getKey(), val);
                        }
                    }
                }

                // update the requestUri
                requestContext.setRequestUri(builder.build());
            } catch (Exception e) {
                logger.warn("error creating URI from string {}",
                        requestUriInfo.getAbsolutePath().toString().replace(
                                requestUriInfo.getPath(), currentPathData.getCurrentUrl()
                        ));

            }

        }
    }


    private VersionUrlConfig.VersionUrlData getCurrentPathData(APIVersion version, String requestedPath) {

        Map<String, List<VersionUrlConfig.VersionUrlData>> versionMap = versionUrlConfig.getVersionMap();

        if (versionMap.containsKey(version.getTransformerSuffix())) {
            for (VersionUrlConfig.VersionUrlData versionData : versionMap.get(version.getTransformerSuffix())) {
                Matcher m = Pattern.compile(versionData.getVersionUrl()).matcher(requestedPath);

                if (m.matches()) {
                    Map<String, String> paramMap = new HashMap<>();

                    for (String name : versionData.getNamedGroups()) {
                        paramMap.put(name, m.group(name));
                    }

                    versionData.setNamedGroupMap(paramMap);

                    return versionData;
                }
            }
        }

        return null;
    }
}
