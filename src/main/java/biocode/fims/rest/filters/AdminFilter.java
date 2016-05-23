package biocode.fims.rest.filters;

import biocode.fims.entities.User;
import biocode.fims.fimsExceptions.ForbiddenRequestException;
import biocode.fims.service.OAuthProviderService;
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
import java.util.List;

/**
 * Custom filter for checking if a user is a project admin
 */
@Provider
@Admin
@Priority(Priorities.AUTHORIZATION)
public class AdminFilter implements ContainerRequestFilter {
    @Context
    HttpServletRequest webRequest;
    @Autowired
    private OAuthProviderService oAuthProviderService;
    @Autowired
    private UserService userService;

    @Override
    public void filter(ContainerRequestContext requestContext)
            throws IOException {
        HttpSession session = webRequest.getSession();
        Object projectAdmin = session.getAttribute("projectAdmin");
        List accessTokenList = requestContext.getUriInfo().getQueryParameters().get("access_token");

        if (accessTokenList != null && !accessTokenList.isEmpty()) {
            User user = oAuthProviderService.getUser((String) accessTokenList.get(0));
            if (user != null) {
                projectAdmin = userService.isProjectAdmin(user);
            }
        }

        if (projectAdmin == null) {
            throw new ForbiddenRequestException("You must be an admin to access this service.");
        }
    }
}
