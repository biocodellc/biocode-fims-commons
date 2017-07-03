package biocode.fims.rest.services.rest;

import biocode.fims.application.config.FimsProperties;
import biocode.fims.rest.FimsService;
import biocode.fims.tools.CachedFile;
import biocode.fims.tools.FileCache;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.File;

/**
 * Biocode-Fims utility services
 *
 * @exclude
 * @resourceTag Utils
 */
public abstract class FimsAbstractUtilsController extends FimsService {

    private final FileCache fileCache;

    @Autowired
    FimsAbstractUtilsController(FileCache fileCache, FimsProperties props) {
        super(props);
        this.fileCache = fileCache;
    }

    @GET
    @Path("/getNAAN")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getNAAN() {
        return Response.ok("{\"naan\": \"" + props.naan() + "\"}").build();
    }

    @GET
    @Path("/file")
    @Produces("application/file")
    public Response getFile(@QueryParam("id") String id) {
        int userId = userContext.getUser() != null ? userContext.getUser().getUserId() : 0;
        CachedFile cf = fileCache.getFile(id, userId);

        if (cf != null) {
            File file = new File(cf.getPath());
            Response.ResponseBuilder response = Response.ok(file);

            String name = !StringUtils.isBlank(cf.getName()) ? cf.getName() : file.getName();
            response.header("Content-Disposition",
                    "attachment; filename=" + name);

            // Return response
            return response.build();
        }

        return Response.noContent().build();
    }
}
