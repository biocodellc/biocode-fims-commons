package biocode.fims.rest.filters;

import biocode.fims.auth.oauth2.OAuthProvider;
import biocode.fims.fimsExceptions.UnauthorizedRequestException;

import javax.annotation.Priority;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import javax.ws.rs.Priorities;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.core.Context;
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
        Object user = session.getAttribute("username");
        List accessTokenList = requestContext.getUriInfo().getQueryParameters().get("access_token");

        if (accessTokenList != null && !accessTokenList.isEmpty()) {
            OAuthProvider provider = new OAuthProvider();
            if (provider.validateToken((String) accessTokenList.get(0)) == null) {
                throw new UnauthorizedRequestException("You must be logged in to access this service",
                        "Invalid/Expired access_token");
            }
        } else if (user == null) {
            throw new UnauthorizedRequestException("You must be logged in to access this service.");
        }
    }
}
