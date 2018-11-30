package biocode.fims.rest.services;

import biocode.fims.application.config.FimsProperties;
import biocode.fims.rest.FimsController;
import biocode.fims.tools.CachedFile;
import biocode.fims.tools.FileCache;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;

import javax.ws.rs.*;
import javax.ws.rs.core.Response;
import java.io.File;

/**
 * File API endpoints
 *
 * @exclude
 * @resourceTag Files
 */
public abstract class BaseFileController extends FimsController {

    private final FileCache fileCache;

    @Autowired
    BaseFileController(FileCache fileCache, FimsProperties props) {
        super(props);
        this.fileCache = fileCache;
    }

    /**
     * Retrieve a file
     *
     * @param id
     * @return
     */
    @GET
    @Path("{id}")
    @Produces("application/file")
    public Response getFile(@PathParam("id") String id) {
        int userId = userContext.getUser() != null ? userContext.getUser().getUserId() : 0;
        CachedFile cf = fileCache.getFile(id, userId);

        if (cf != null) {
            File file = new File(cf.getPath());
            Response.ResponseBuilder response = Response.ok(file);

            String name = !StringUtils.isBlank(cf.getName()) ? cf.getName() : file.getName();
            response.header("Content-Disposition",
                    "attachment; filename=\"" + name + "\"");

            // Return response
            return response.build();
        }

        return Response.noContent().build();
    }
}
