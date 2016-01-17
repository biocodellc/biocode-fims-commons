package biocode.fims.rest.services;

import biocode.fims.bcid.ResourceTypes;
import biocode.fims.rest.FimsService;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 * REST interface for creating elements, to be called from the interface or other consuming applications.
 */
@Path("resourceTypes")
public class ResourceService extends FimsService {

    /**
     * get all resourceTypes minus Dataset
     *
     * @return String with JSON response
     */
    @GET
    @Path("/minusDataset")
    @Produces(MediaType.APPLICATION_JSON)
    public Response jsonSelectOptions() {
        ResourceTypes rts = new ResourceTypes();
        return Response.ok(rts.getAllButDatasetAsJSON().toJSONString()).build();
    }

    /**
     * Get all resourceTypes
     *
     * @return
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response resourceTypes() {
        ResourceTypes rts = new ResourceTypes();
        return Response.ok(rts.getAllAsJSON().toJSONString()).build();
    }
}