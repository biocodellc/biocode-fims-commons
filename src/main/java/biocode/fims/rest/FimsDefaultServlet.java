package biocode.fims.rest;

import org.eclipse.jetty.servlet.DefaultServlet;

import javax.servlet.*;
import javax.servlet.http.*;
import java.io.File;
import java.io.IOException;

/**
 * This servlet is used with angularJs for overriding the jetty servlet responsible for serving static files.
 * When a request is received, we check to see if the file exists in the filesystem. If not, then we want to
 * defer to angularjs for routing. Otherwise, we will have 404's when directly calling an angularjs page.
 *
 */
public class FimsDefaultServlet extends DefaultServlet {

    @Override
    protected void service(HttpServletRequest request, HttpServletResponse response ) throws ServletException, IOException {
        // Check to see if the requested file exists. If not, then we need to we need to show index.html and let
        // angular JS router do the work
        String redirectRoute = "/index.html";

        String absoluteFilePath = getServletContext().getRealPath(request.getPathInfo());
        File file = new File(absoluteFilePath);

        if (!file.exists()) {
            RequestDispatcher dispatcher = request.getRequestDispatcher(redirectRoute);
            response.reset();

            dispatcher.forward(request, response);
        }

        super.service(request, response);
    }


}
