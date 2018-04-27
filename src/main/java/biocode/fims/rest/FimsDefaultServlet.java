package biocode.fims.rest;

import org.eclipse.jetty.servlet.DefaultServlet;

import javax.servlet.*;
import javax.servlet.http.*;
import java.io.File;
import java.io.IOException;

/**
 * This servlet is used when Jersey is registered at the same path as we may need to serve resources under.
 * For ex. if Jersey is registered a /* and we want to serve /doc/test.json, by default, Jersey will throw
 * a 404 b/c it can't match the rest service. Instead, we can register the Jersey Application as a filter
 * and set the <initParam>jersey.config.servlet.filter.forwardOn404</initParam> to true, which will forward
 * the request to jetty if. The disadvantage is that jetty will return a 204 for the request /doc. This
 * filter will throw a 404, unless the specific file exists on the server
 *
 */
public class FimsDefaultServlet extends DefaultServlet {

    @Override
    protected void service(HttpServletRequest request, HttpServletResponse response ) throws ServletException, IOException {
        // Check to see if the requested file exists. If not, throw a 404
        String absoluteFilePath = getServletContext().getRealPath(request.getPathInfo());
        File file = new File(absoluteFilePath);

        if (!file.exists() || file.isDirectory()) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        super.service(request, response);
    }
}
