package biocode.fims.rest;

import biocode.fims.auth.oauth2.OAuthProvider;
import biocode.fims.bcid.BcidDatabase;
import biocode.fims.settings.SettingsManager;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import java.io.File;
import java.util.List;

/**
 * An abstract class that sets the necessary information when communicating with Biocode-Fims services
 */
public abstract class FimsService {
    @Context
    protected ServletContext context;
    @Context
    protected HttpHeaders headers;
    protected HttpSession session;
    protected String username;
    protected Integer userId;
    @QueryParam("access_token")
    protected String accessToken;

    protected static SettingsManager sm;
    protected static String appRoot;
    protected static Boolean ignoreUser;

    static {
        sm = SettingsManager.getInstance();
        appRoot = sm.retrieveValue("appRoot", null);
        ignoreUser = Boolean.valueOf(sm.retrieveValue("ignoreUser"));
    }

    public String uploadPath() {
        return context.getRealPath(sm.retrieveValue("uploadDir")) + File.separator;
    }

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
            OAuthProvider provider = new OAuthProvider();
            username = provider.validateToken(accessToken);
            userId = BcidDatabase.getUserId(username);
        } else {
            username = (String) session.getAttribute("username");
            userId = (Integer) session.getAttribute("userId");
        }
    }
}
