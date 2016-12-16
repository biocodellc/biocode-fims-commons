package biocode.fims.rest;

import biocode.fims.settings.SettingsManager;
import org.springframework.beans.factory.annotation.Autowired;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.UriInfo;
import java.io.File;

/**
 * An abstract class that sets the necessary information when communicating with Biocode-Fims services
 */
public abstract class FimsService {
    @Autowired
    protected UserContext userContext;

    @Context
    public UriInfo uriInfo;
    @Context
    protected ServletContext context;
    @Context
    protected HttpHeaders headers;

    protected HttpSession session;
    protected final String appRoot;
    protected final boolean ignoreUser;

    protected final SettingsManager settingsManager;

    public FimsService(SettingsManager settingsManager) {
        this.settingsManager = settingsManager;

        appRoot = settingsManager.retrieveValue("appRoot", null);
        ignoreUser = Boolean.valueOf(settingsManager.retrieveValue("ignoreUser", "false"));
    }

    protected String uploadPath() {
        return context.getRealPath(settingsManager.retrieveValue("uploadDir")) + File.separator;
    }

    public HttpHeaders getHeaders() { return headers; }

    @Context
    public void setSessionVariables(HttpServletRequest request) {
        session = request.getSession();
    }
}
