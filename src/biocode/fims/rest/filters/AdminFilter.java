package biocode.fims.rest.filters;

import biocode.fims.auth.Authorizer;
import biocode.fims.auth.oauth2.OAuthProvider;
import biocode.fims.fimsExceptions.ForbiddenRequestException;

import javax.annotation.Priority;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import javax.ws.rs.Priorities;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.core.Context;
import javax.ws.rs.ext.Provider;
import java.io.IOException;

/**
 * Custom filter for checking if a user is a project admin
 */
@Provider
@Admin
@Priority(Priorities.AUTHORIZATION)
public class AdminFilter implements ContainerRequestFilter {
    @Context
    HttpServletRequest webRequest;

    @Override
    public void filter(ContainerRequestContext requestContext)
            throws IOException {
        HttpSession session = webRequest.getSession();
        Object projectAdmin = session.getAttribute("projectAdmin");
        Object accessToken = requestContext.getUriInfo().getQueryParameters().get("access_token");

        if (accessToken != null) {
            OAuthProvider provider = new OAuthProvider();
            String username = provider.validateToken(accessToken.toString());
            Authorizer authorizer = new Authorizer();
            projectAdmin = authorizer.userProjectAdmin(username);
            authorizer.close();
            provider.close();
        }

        if (projectAdmin == null) {
            throw new ForbiddenRequestException("You must be an admin to access this service.");
        }
    }
}
