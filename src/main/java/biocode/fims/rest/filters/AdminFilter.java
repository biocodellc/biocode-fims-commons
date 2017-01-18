package biocode.fims.rest.filters;

import biocode.fims.fimsExceptions.ForbiddenRequestException;
import biocode.fims.rest.UserContext;
import biocode.fims.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;

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
    @Autowired
    private UserService userService;
    @Autowired
    private UserContext userContext;

    @Override
    public void filter(ContainerRequestContext requestContext)
            throws IOException {
        if (userContext.getUser() == null || !userService.isAProjectAdmin(userContext.getUser())) {
            throw new ForbiddenRequestException("You must be an admin to access this service.");
        }
    }
}
