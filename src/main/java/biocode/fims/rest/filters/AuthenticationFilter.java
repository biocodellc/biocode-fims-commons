package biocode.fims.rest.filters;

import biocode.fims.entities.User;
import biocode.fims.rest.UserContext;
import biocode.fims.service.OAuthProviderService;
import org.springframework.beans.factory.annotation.Autowired;

import javax.annotation.Priority;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import javax.ws.rs.Priorities;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.ResourceInfo;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.ext.Provider;
import java.io.IOException;
import java.util.List;

/**
 * Custom filter to set the {@link UserContext#user} for the incoming request
 * @author RJ Ewing
 */
@Provider
@Priority(Priorities.AUTHENTICATION)
public class AuthenticationFilter implements ContainerRequestFilter {
    @Context
    private ResourceInfo resourceInfo;
    @Context
    private HttpServletRequest webRequest;
    @Autowired
    private UserContext userContext;

    @Autowired
    private OAuthProviderService providerService;

    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        String accessToken = null;
        List accessTokenList = requestContext.getUriInfo().getQueryParameters().get("access_token");

        String authHeader = webRequest.getHeader(HttpHeaders.AUTHORIZATION);

        if (accessTokenList != null && !accessTokenList.isEmpty())
            accessToken = (String) accessTokenList.get(0);

        // If accessToken is null, check if the HTTP Authorization header is present and formatted correctly
        if (accessToken == null && authHeader != null && authHeader.startsWith("Bearer ")) {
            // Extract the token from the HTTP Authorization header
            accessToken = authHeader.substring("Bearer".length()).trim();
        }

        if (accessToken != null && !accessToken.isEmpty()) {
            userContext.setUser(providerService.getUser(accessToken));
        } else {
            HttpSession session = webRequest.getSession();

            if (session.getAttribute("user") != null) {
                userContext.setUser((User) session.getAttribute("user"));
            }
        }
    }
}
