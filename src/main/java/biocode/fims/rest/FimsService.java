package biocode.fims.rest;

import biocode.fims.application.config.FimsProperties;
import org.springframework.beans.factory.annotation.Autowired;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.UriInfo;

/**
 * An abstract class that sets the necessary information when communicating with Biocode-Fims services
 */
public abstract class FimsService {
    @Autowired
    protected UserContext userContext;

    @Context
    public UriInfo uriInfo;
    @Context
    protected HttpHeaders headers;

    protected HttpSession session;

    protected final FimsProperties props;

    public FimsService(FimsProperties props) {
        this.props = props;
    }

    protected String defaultOutputDirectory() {
        return System.getProperty("java.io.tmpdir");
    }

    public HttpHeaders getHeaders() { return headers; }

    @Context
    public void setSessionVariables(HttpServletRequest request) {
        session = request.getSession();
    }
}
