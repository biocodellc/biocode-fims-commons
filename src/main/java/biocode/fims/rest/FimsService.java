package biocode.fims.rest;

import biocode.fims.entities.User;
import biocode.fims.service.OAuthProviderService;
import biocode.fims.settings.SettingsManager;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import javax.ws.rs.QueryParam;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.UriInfo;
import java.io.File;

/**
 * An abstract class that sets the necessary information when communicating with Biocode-Fims services
 */
public abstract class FimsService {
    @Context
    public UriInfo uriInfo;
    @Context
    protected ServletContext context;
    @Context
    protected HttpHeaders headers;
    protected HttpSession session;
    protected User user;
    @QueryParam("access_token")
    protected String accessToken;
    protected final String appRoot;
    protected final boolean ignoreUser;

    private final OAuthProviderService providerService;
    protected final SettingsManager settingsManager;

    public FimsService(OAuthProviderService providerService, SettingsManager settingsManager) {
        this.providerService = providerService;
        this.settingsManager = settingsManager;

        appRoot = settingsManager.retrieveValue("appRoot", null);
        ignoreUser = Boolean.valueOf(settingsManager.retrieveValue("ignoreUser", "false"));
    }

    public String uploadPath() {
        return context.getRealPath(settingsManager.retrieveValue("uploadDir")) + File.separator;
    }

    public HttpHeaders getHeaders() { return headers; }

    @Context
    public void setSessionVariables(HttpServletRequest request) {
        session = request.getSession();
        String authHeader = headers.getHeaderString(HttpHeaders.AUTHORIZATION);

        // If accessToken is null, check if the HTTP Authorization header is present and formatted correctly
        if (accessToken == null && authHeader != null && authHeader.startsWith("Bearer ")) {
            // Extract the token from the HTTP Authorization header
            accessToken = authHeader.substring("Bearer".length()).trim();
        }

        if (accessToken != null && !accessToken.isEmpty()) {
            user = providerService.getUser(accessToken);
        } else {
            user = (User) session.getAttribute("user");
        }
    }
}
