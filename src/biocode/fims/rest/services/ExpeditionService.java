package biocode.fims.rest.services;

import biocode.fims.bcid.ExpeditionMinter;
import biocode.fims.bcid.ProjectMinter;
import biocode.fims.bcid.Resolver;
import biocode.fims.fimsExceptions.BadRequestException;
import biocode.fims.fimsExceptions.FimsException;
import biocode.fims.fimsExceptions.ForbiddenRequestException;
import biocode.fims.rest.FimsService;
import biocode.fims.rest.filters.Admin;
import biocode.fims.rest.filters.Authenticated;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;

/**
 * REST interface calls for working with expeditions.  This includes creating, updating and deleting expeditions.
 */
@Path("expeditions")
public class ExpeditionService extends FimsService {

    private static Logger logger = LoggerFactory.getLogger(ExpeditionService.class);

    /**
     * Service for a user to mint a new expedition
     *
     * @param expeditionCode
     * @param expeditionTitle
     * @param projectId
     * @param isPublic
     *
     * @return
     */
    @POST
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces(MediaType.APPLICATION_JSON)
    @Authenticated
    public Response mint(@FormParam("expeditionCode") String expeditionCode,
                         @FormParam("expeditionTitle") String expeditionTitle,
                         @FormParam("projectId") Integer projectId,
                         @FormParam("public") Boolean isPublic) {

        if (isPublic == null) {
            isPublic = true;
        }
        ExpeditionMinter expedition = new ExpeditionMinter();

        try {
            // Mint a expedition
            Integer expeditionId = expedition.mint(
                    expeditionCode,
                    expeditionTitle,
                    userId,
                    projectId,
                    isPublic
            );

            return Response.ok(expedition.getMetadata(expeditionId).toJSONString()).build();
        } catch (FimsException e) {
            throw new BadRequestException(e.getMessage());
        } finally {
            expedition.close();
        }
    }

    /**
     * Given a graph name, return metadata
     *
     * @param graph
     *
     * @return
     */
    @GET
    @Path("/graphMetadata/{graph}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getGraphMetadata(@PathParam("graph") String graph) {
        ExpeditionMinter e = new ExpeditionMinter();
        JSONObject response = e.getGraphMetadata(graph);
        e.close();
        return Response.ok(response.toJSONString()).build();
    }

    /**
     * Given a expedition code and a resource alias, return a BCID
     *
     * @param expedition
     * @param resourceAlias
     *
     * @return
     */
    @GET
    @Path("/{projectId}/{expedition}/{resourceAlias}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response fetchAlias(@PathParam("expedition") String expedition,
                               @PathParam("projectId") Integer projectId,
                               @PathParam("resourceAlias") String resourceAlias) {
        try {
            expedition = URLDecoder.decode(expedition, "utf-8");
        } catch (UnsupportedEncodingException e) {
            logger.warn("UnsupportedEncodingException in ExpeditionService.fetchAlias method.", e);
        }

        Resolver r = new Resolver(expedition, projectId, resourceAlias);
        String response = r.getIdentifier();
        r.close();
        if (response == null) {
            return Response.status(Response.Status.NO_CONTENT).entity("{\"identifier\": \"\"}").build();
        } else {
            return Response.ok("{\"identifier\": \"" + response + "\"}").build();
        }
    }

    /**
     * Service to retrieve all of the project's expeditions. For use by project admin only.
     *
     * @param projectId
     *
     * @return
     */
    @GET
    @Admin
    @Authenticated
    @Path("/admin/list/{projectId}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getExpeditions(@PathParam("projectId") Integer projectId) {
        ProjectMinter projectMinter = new ProjectMinter();
        if (!projectMinter.isProjectAdmin(username, projectId)) {
            throw new ForbiddenRequestException("You must be this project's admin in order to view its expeditions.");
        }

        ExpeditionMinter e = new ExpeditionMinter();
        JSONArray expeditions = e.getExpeditions(projectId, username);
        e.close();
        return Response.ok(expeditions.toJSONString()).build();
    }


    /**
     * Service to set/unset the public attribute of a set of expeditions specified in a MultivaluedMap
     * The expeditionId's are specified simply by their internal expeditionId code
     *
     * @param data
     *
     * @return
     */
    @POST
    @Authenticated
    @Admin
    @Path("/admin/updateStatus")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public Response publicExpeditions(MultivaluedMap<String, String> data) {
        Integer projectId = new Integer(data.remove("projectId").get(0));

        ProjectMinter p = new ProjectMinter();
        Boolean projectAdmin = p.isProjectAdmin(username, projectId);
        p.close();

        if (!projectAdmin) {
            throw new ForbiddenRequestException("You must be this project's admin in order to update a project expedition's public status.");
        }
        ExpeditionMinter e = new ExpeditionMinter();

        e.updateExpeditionsPublicStatus(data, projectId);
        e.close();
        return Response.ok("{\"success\": \"successfully updated.\"}").build();
    }

    /**
     * Service to update a single expedition Bcid
     *
     * @param projectId
     * @param expeditionCode
     * @param publicStatus
     *
     * @return
     */
    @GET
    @Authenticated
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Path("/updateStatus/{projectId}/{expeditionCode}/{publicStatus}")
    public Response publicExpedition(
            @PathParam("projectId") Integer projectId,
            @PathParam("expeditionCode") String expeditionCode,
            @PathParam("publicStatus") Boolean publicStatus) {

        ExpeditionMinter e = new ExpeditionMinter();

        // Update the expedition public status for what was just passed in

        if (e.updateExpeditionPublicStatus(userId, expeditionCode, projectId, publicStatus)) {
            e.close();
            return Response.ok("{\"success\": \"successfully updated.\"}").build();
        } else {
            e.close();
            return Response.ok("{\"success\": \"nothing to update.\"}").build();
        }
    }

}

