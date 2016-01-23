package biocode.fims.rest.services.rest;

import biocode.fims.rest.FimsService;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 * Biocode-Fims utility services
 */
@Path("utils/")
public class Utils extends FimsService {

    @GET
    @Path("/getNAAN")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getNAAN() {
        String naan = sm.retrieveValue("naan");

        return Response.ok("{\"naan\": \"" + naan + "\"}").build();
    }
}
