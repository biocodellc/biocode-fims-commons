package biocode.fims.rest.filters;

import biocode.fims.auth.oauth2.OAuthProvider;
import biocode.fims.entities.User;
import biocode.fims.fimsExceptions.UnauthorizedRequestException;

import javax.annotation.Priority;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import javax.ws.rs.Priorities;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.ext.Provider;
import java.io.IOException;
import java.util.List;

/**
 * Custom filter for checking if a user is logged in
 */
@Provider
@Authenticated
@Priority(Priorities.AUTHENTICATION)
public class AuthenticatedFilter implements ContainerRequestFilter {
    @Context
    HttpServletRequest webRequest;

    @Override
    public void filter(ContainerRequestContext requestContext)
        throws IOException {
        HttpSession session = webRequest.getSession();
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

        if (accessToken != null) {
            OAuthProvider provider = new OAuthProvider();
            if (provider.validateToken(accessToken) == null) {
                throw new UnauthorizedRequestException("You must be logged in to access this service",
                        "Invalid/Expired access_token");
            }
        } else if (session.getAttribute("user") != null) {
            throw new UnauthorizedRequestException("You must be logged in to access this service.");
        }
    }
}
