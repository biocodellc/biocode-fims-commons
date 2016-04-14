package biocode.fims.rest.services.rest;

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
import java.net.URI;
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
                         @FormParam("webAddress") String webAddress,
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
                    webAddress,
                    isPublic
            );

            return Response.ok(expedition.getMetadata(expeditionId).toJSONString()).build();
        } catch (FimsException e) {
            throw new BadRequestException(e.getMessage());
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
        return Response.ok(response.toJSONString()).build();
    }

    /**
     * Given a expedition code and a resource alias, return a BCID
     *
     * @param expeditionCode
     * @param resourceAlias
     *
     * @return
     */
    @GET
    @Path("/{projectId}/{expeditionCode}/{resourceAlias}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response fetchAlias(@PathParam("expeditionCode") String expeditionCode,
                               @PathParam("projectId") Integer projectId,
                               @PathParam("resourceAlias") String resourceAlias) {
        try {
            expeditionCode = URLDecoder.decode(expeditionCode, "utf-8");
        } catch (UnsupportedEncodingException e) {
            logger.warn("UnsupportedEncodingException in ExpeditionService.fetchAlias method.", e);
        }

        Resolver r = new Resolver(expeditionCode, projectId, resourceAlias);
        URI identifier = r.getBcid().getIdentifier();
        if (identifier == null) {
            return Response.status(Response.Status.NO_CONTENT).entity("{\"identifier\": \"\"}").build();
        } else {
            return Response.ok("{\"identifier\": \"" + identifier + "\"}").build();
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

        if (!projectAdmin) {
            throw new ForbiddenRequestException("You must be this project's admin in order to update a project expedition's public status.");
        }
        ExpeditionMinter e = new ExpeditionMinter();

        e.updateExpeditionsPublicStatus(data, projectId);
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
            return Response.ok("{\"success\": \"successfully updated.\"}").build();
        } else {
            return Response.ok("{\"success\": \"nothing to update.\"}").build();
        }
    }

    /**
     * validateExpedition service checks the status of a new expedition code on the server and directing consuming
     * applications on whether this user owns the expedition and if it exists within an project or not.
     * Responses are error, update, or insert (first term followed by a colon)
     *
     * @param expeditionCode
     * @param projectId
     * @param ignoreUser     if specified as true then we don't perform a check on what user owns the dataset
     *
     * @return
     */
    @GET
    @Authenticated
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/validate/{projectId}/{expeditionCode}")
    public Response validate(@PathParam("expeditionCode") String expeditionCode,
                             @PathParam("projectId") Integer projectId,
                             @QueryParam("ignoreUser") Boolean ignoreUser) {
        String username;

        // Default the lIgnore_user variable to false.  Set if true only if user specified it
        Boolean lIgnoreUser = false;
        if (ignoreUser != null && ignoreUser) {
            lIgnoreUser = true;
        }

        // Decipher the expedition code
        try {
            expeditionCode = URLDecoder.decode(expeditionCode, "utf-8");
        } catch (UnsupportedEncodingException e) {
            logger.warn("UnsupportedEncodingException in expeditionService.mint method.", e);
        }

        // Create the expeditionMinter object so we can test and validate it
        ExpeditionMinter expeditionMinter = new ExpeditionMinter();
        ProjectMinter projectMinter = new ProjectMinter();

        //Check that the user exists in this project
        if (!projectMinter.userExistsInProject(userId, projectId)) {
            // If the user isn't in the project, then we can't update or create a new expedition
            throw new ForbiddenRequestException("User is not authorized to update/create expeditions in this project.");
        }

        // If specified, ignore the user.. simply figure out whether we're updating or inserting
        if (lIgnoreUser) {
            if (expeditionMinter.expeditionExistsInProject(expeditionCode, projectId)) {
                return Response.ok("{\"update\": \"update this expedition\"}").build();
            } else {
                return Response.ok("{\"insert\": \"insert new expedition\"}").build();
            }
        }

        // Else, pay attention to what user owns the initial project
        else {
            if (expeditionMinter.userOwnsExpedition(userId, expeditionCode, projectId)) {
                // If the user already owns the expedition, then great--- this is an update
                return Response.ok("{\"update\": \"user owns this expedition\"}").build();
                // If the expedition exists in the project but the user does not own the expedition then this means we can't
            } else if (expeditionMinter.expeditionExistsInProject(expeditionCode, projectId)) {
                throw new ForbiddenRequestException("The expedition code '" + expeditionCode +
                        "' exists in this project already and is owned by another user. " +
                        "Please choose another expedition code.");
            } else {
                return Response.ok("{\"insert\": \"the expedition does not exist with project and nobody owns it\"}").build();
            }
        }
    }

    @POST
    @Authenticated
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/associate")
    public Response associate(@FormParam("expeditionCode") String expeditionCode,
                              @FormParam("bcid") String identifier,
                              @FormParam("projectId") Integer projectId) {
        ExpeditionMinter expedition = new ExpeditionMinter();
        if (identifier == null || expeditionCode == null) {
            throw new BadRequestException("bcid and expeditionCode must not be null.");
        }
        expedition.attachReferenceToExpedition(expeditionCode, identifier, projectId);

        return Response.ok("{\"success\": \"Data Elements Root: " + expeditionCode + "\"}").build();
    }

}

