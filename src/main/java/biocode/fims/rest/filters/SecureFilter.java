package biocode.fims.rest.filters;

import biocode.fims.settings.SettingsManager;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;

/**
 * Filter which checks for the user in the session, if no user is present, the redirect to login page.
 * The login page is determined b first fetching the properties file name from the servlet-context init-param
 * (propsFilename). Then it looks in the props file for a loginPageUrl
 *
 */
public class SecureFilter implements Filter {
    private FilterConfig fc = null;

    private static SettingsManager sm;
    private static String loginPageUrl;

    public void init (FilterConfig fc)
        throws ServletException {
        this.fc = fc;
    }

    public void destroy() {
        this.fc = null;
    }

    public void doFilter (ServletRequest req,
                          ServletResponse res,
                          FilterChain filterchain)
        throws IOException, ServletException {

        if (sm == null) {
            sm = SettingsManager.getInstance();
        }
        if (loginPageUrl == null) {
            loginPageUrl = sm.retrieveValue("loginPageUrl");
        }

        HttpServletRequest request = (HttpServletRequest) req;
        HttpServletResponse response = (HttpServletResponse) res;

        HttpSession session = request.getSession();

        if (session.getAttribute("username") == null) {
            response.sendRedirect(loginPageUrl);
        }
        filterchain.doFilter(req, res);
    }
}
