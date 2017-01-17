package biocode.fims.rest.services.rest;

import biocode.fims.bcid.ExpeditionMinter;
import biocode.fims.bcid.ProjectMinter;
import biocode.fims.config.ConfigurationFileFetcher;
import biocode.fims.digester.Mapping;
import biocode.fims.entities.Bcid;
import biocode.fims.entities.Expedition;
import biocode.fims.fimsExceptions.BadRequestException;
import biocode.fims.fimsExceptions.ForbiddenRequestException;
import biocode.fims.rest.FimsService;
import biocode.fims.rest.filters.Admin;
import biocode.fims.rest.filters.Authenticated;
import biocode.fims.serializers.Views;
import biocode.fims.service.ExpeditionService;
import biocode.fims.service.OAuthProviderService;
import biocode.fims.settings.SettingsManager;
import com.fasterxml.jackson.annotation.JsonView;
import org.apache.commons.lang.StringUtils;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.web.util.UriComponentsBuilder;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLDecoder;

/**
 * REST interface calls for working with expeditions.  This includes creating, updating and deleting expeditions.
 * @exclude
 */
public abstract class FimsAbstractExpeditionController extends FimsService {

    private static Logger logger = LoggerFactory.getLogger(FimsAbstractExpeditionController.class);
    private final ExpeditionService expeditionService;

    @Autowired
    public FimsAbstractExpeditionController(ExpeditionService expeditionService, SettingsManager settingsManager) {
        super(settingsManager);
        this.expeditionService = expeditionService;
    }

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
    @JsonView(Views.Detailed.class)
    @POST
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces(MediaType.APPLICATION_JSON)
    @Authenticated
    public Response mint(@FormParam("expeditionCode") String expeditionCode,
                         @FormParam("expeditionTitle") String expeditionTitle,
                         @FormParam("projectId") Integer projectId,
                         @FormParam("webAddress") String webAddress,
                         @FormParam("public") @DefaultValue("true") Boolean isPublic) {
        URI uri;

        File configFile = new ConfigurationFileFetcher(projectId, uploadPath(), true).getOutputFile();

        Mapping mapping = new Mapping();
        mapping.addMappingRules(configFile);

        Expedition expedition = new Expedition.ExpeditionBuilder(expeditionCode)
                .expeditionTitle(expeditionTitle)
                .isPublic(isPublic)
                .build();

        if (!StringUtils.isEmpty(webAddress)) {
            uri = UriComponentsBuilder.fromUriString(webAddress).build().toUri();
        } else {
            uri = null;
        }
        expeditionService.create(expedition, userContext.getUser().getUserId(), projectId, uri, mapping);

        return Response.ok(expedition).build();
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
     * @param conceptAlias
     *
     * @return
     */
    @GET
    @Path("/{projectId}/{expeditionCode}/{conceptAlias}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response fetchAlias(@PathParam("expeditionCode") String expeditionCode,
                               @PathParam("projectId") Integer projectId,
                               @PathParam("conceptAlias") String conceptAlias) {
        try {
            expeditionCode = URLDecoder.decode(expeditionCode, "utf-8");
        } catch (UnsupportedEncodingException e) {
            logger.warn("UnsupportedEncodingException in FimsAbstractExpeditionController.fetchAlias method.", e);
        }

        try {
            Bcid bcid = expeditionService.getEntityBcid(expeditionCode, projectId, conceptAlias);

            return Response.ok("{\"identifier\": \"" + bcid.getIdentifier() + "\"}").build();

        } catch (EmptyResultDataAccessException e) {
            return Response.status(Response.Status.NO_CONTENT).entity("{\"identifier\": \"\"}").build();
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
        if (!projectMinter.isProjectAdmin(userContext.getUser().getUsername(), projectId)) {
            throw new ForbiddenRequestException("You must be this project's admin in order to view its expeditions.");
        }

        ExpeditionMinter e = new ExpeditionMinter();
        JSONArray expeditions = e.getExpeditions(projectId, userContext.getUser().getUsername());
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
        Boolean projectAdmin = p.isProjectAdmin(userContext.getUser().getUsername(), projectId);

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

        if (e.updateExpeditionPublicStatus(userContext.getUser().getUserId(), expeditionCode, projectId, publicStatus)) {
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
     * @param ignoreUser     if specified as true then we don't perform a check on what user owns the fimsMetadata
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
        if (!projectMinter.userExistsInProject(userContext.getUser().getUserId(), projectId)) {
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
            if (expeditionMinter.userOwnsExpedition(userContext.getUser().getUserId(), expeditionCode, projectId)) {
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

