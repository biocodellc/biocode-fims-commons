package biocode.fims.rest.filters;

import biocode.fims.fimsExceptions.UnauthorizedRequestException;
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
 * Custom filter for checking if a user is logged in
 */
@Provider
@Authenticated
@Priority(Priorities.AUTHORIZATION)
public class AuthenticatedFilter implements ContainerRequestFilter {
    @Autowired
    private UserContext userContext;

    @Override
    public void filter(ContainerRequestContext requestContext)
            throws IOException {
        if (userContext.getUser() == null) {
            throw new UnauthorizedRequestException("You must be logged in to access this service",
                    "Possible Invalid/Expired access_token");
        }
    }
}
