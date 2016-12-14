package biocode.fims.rest.filters;

import biocode.fims.fimsExceptions.UnauthorizedRequestException;
import biocode.fims.rest.UserContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import javax.annotation.Priority;
import javax.ws.rs.Priorities;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.ext.Provider;
import java.io.IOException;

/**
 * Custom filter for checking if a user is logged in and matches {userId} pathParameter
 */
@Provider
@AuthenticatedUserResource
@Priority(Priorities.AUTHORIZATION)
public class AuthenticatedUserResourceFilter implements ContainerRequestFilter {
    private final static Logger logger = LoggerFactory.getLogger(AuthenticatedUserResourceFilter.class);
    @Autowired
    private UserContext userContext;

    @Override
    public void filter(ContainerRequestContext requestContext)
            throws IOException {
        UriInfo uriInfo = requestContext.getUriInfo();

        if (userContext.getUser() == null) {
            throw new UnauthorizedRequestException("You must be logged in to access this service",
                    "Possible Invalid/Expired access_token");
        }

        if (!uriInfo.getPathParameters().containsKey("userId")) {
            logger.debug("missing {userId} path param");
        } else if (userContext.getUser().getUserId() != Integer.parseInt(uriInfo.getPathParameters().get("userId").get(0))) {
            throw new UnauthorizedRequestException("The path {userId} does not match the logged in userId");

        }
    }
}
