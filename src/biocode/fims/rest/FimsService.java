package biocode.fims.rest;

import biocode.fims.auth.oauth2.OAuthProvider;
import biocode.fims.bcid.Database;
import biocode.fims.settings.SettingsManager;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import java.io.File;

/**
 * An abstract class that sets the necessary information when communicating with Biocode-Fims services
 */
public abstract class FimsService {
    @Context
    protected ServletContext context;
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
        if (accessToken != null && !accessToken.isEmpty()) {
            OAuthProvider provider = new OAuthProvider();
            username = provider.validateToken(accessToken);
            Database db = new Database();
            userId = db.getUserId(username);
        } else {
            username = (String) session.getAttribute("username");
            userId = (Integer) session.getAttribute("userId");
        }
    }
}
