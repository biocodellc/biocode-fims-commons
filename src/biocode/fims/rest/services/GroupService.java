package biocode.fims.rest.services;

import biocode.fims.bcid.Bcid;
import biocode.fims.bcid.BcidMinter;
import biocode.fims.bcid.ExpeditionMinter;
import biocode.fims.bcid.Renderer.JSONRenderer;
import biocode.fims.bcid.Renderer.Renderer;
import biocode.fims.bcid.ResourceTypes;
import biocode.fims.fimsExceptions.BadRequestException;
import biocode.fims.rest.FimsService;
import biocode.fims.rest.filters.Authenticated;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.Hashtable;

/**
 * REST interface calls for working with data groups.    This includes creating a group, looking up
 * groups by user associated with them, and JSON representation of group metadata.
 */
@Path("groupService")
public class GroupService extends FimsService {

    final static Logger logger = LoggerFactory.getLogger(GroupService.class);

    /**
     * Load settings manager, set ontModelSpec.
     */

    /**
     * Create a data group
     *
     * @param doi
     * @param webAddress
     * @param title
     *
     * @return
     */
    @POST
    @Authenticated
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces(MediaType.APPLICATION_JSON)
    public Response mint(@FormParam("doi") String doi,
                         @FormParam("webAddress") String webAddress,
                         @FormParam("graph") String graph,
                         @FormParam("title") String title,
                         @FormParam("resourceType") String resourceTypeString,
                         @FormParam("resourceTypesMinusDataset") Integer resourceTypesMinusDataset,
                         @FormParam("suffixPassThrough") String stringSuffixPassThrough,
                         @FormParam("finalCopy") @DefaultValue("false") Boolean finalCopy) {

        // If resourceType is specified by an integer, then use that to set the String resourceType.
        // If the user omits
        try {
            if (resourceTypesMinusDataset != null && resourceTypesMinusDataset > 0) {
                resourceTypeString = new ResourceTypes().get(resourceTypesMinusDataset).uri;
            }
        } catch (IndexOutOfBoundsException e) {
            throw new BadRequestException("BCID System Unable to set resource type",
                    "There was an error retrieving the resource type uri. Did you provide a valid resource type?");
        }

        if (resourceTypeString == null || resourceTypeString.isEmpty()) {
            throw new BadRequestException("ResourceType is required");
        }

        Boolean suffixPassthrough = false;
        if ((stringSuffixPassThrough != null && !stringSuffixPassThrough.isEmpty()) &&
                (stringSuffixPassThrough.equalsIgnoreCase("true") || stringSuffixPassThrough.equalsIgnoreCase("on"))) {
            suffixPassthrough = true;

        }

        if (suffixPassthrough && (webAddress == null || webAddress.isEmpty())) {
            throw new BadRequestException("You must provide a Target URL if following suffixes.");
        }

        // Create a Bcid
        // Mint the data group
        BcidMinter bcidMinter = new BcidMinter(Boolean.valueOf(sm.retrieveValue("ezidRequests")));
        if (title == null || title.isEmpty()) {
            title = resourceTypeString;
        }
        String identifier = bcidMinter.createEntityBcid(new Bcid(userId, resourceTypeString, title,
                webAddress, graph, doi, finalCopy, suffixPassthrough));
        bcidMinter.close();

        return Response.ok("{\"identifier\": \"" + identifier + "\"}").build();
    }

    /**
     * Return a JSON representation of bcids metadata
     *
     * @param bcid_id
     *
     * @return
     */
    @GET
    @Path("/metadata/{bcid_id}")
    @Produces(MediaType.APPLICATION_JSON)
    public String run(@PathParam("bcid_id") Integer bcid_id) {
        Bcid bcid = new Bcid(bcid_id);
        Renderer renderer = new JSONRenderer();

        return "[" + renderer.render(bcid) + "]";
    }

    /**
     * Return JSON response showing data groups available to this user
     *
     * @return String with JSON response
     */
    @GET
    @Authenticated
    @Path("/list")
    @Produces(MediaType.APPLICATION_JSON)
    public Response bcidList() {
        BcidMinter d = new BcidMinter();
        String response = d.bcidList(username);
        d.close();

        return Response.ok(response).build();
    }

    /**
     * Return HTML response showing a table of groups belonging to this user
     *
     * @return String with HTML response
     */
    @GET
    @Authenticated
    @Path("/listUserBCIDsAsTable")
    @Produces(MediaType.TEXT_HTML)
    public Response listUserBCIDsAsTable() {
        BcidMinter d = new BcidMinter();
        String response = d.bcidTable(username);
        d.close();

        return Response.ok(response).build();
    }

    /**
     * Return HTML response showing a table of groups belonging to this user
     *
     * @return String with HTML response
     */
    @GET
    @Authenticated
    @Path("/listUserExpeditionsAsTable")
    @Produces(MediaType.TEXT_HTML)
    public Response listUserExpeditionsAsTable() {
        ExpeditionMinter e = new ExpeditionMinter();
        String tablename = e.expeditionTable(username);
        e.close();
        return Response.ok(tablename).build();
    }

    /**
     * returns an HTML table used to edit a Bcid's configuration.
     *
     * @param identifier
     *
     * @return
     */
    @GET
    @Authenticated
    @Path("/dataGroupEditorAsTable")
    @Produces(MediaType.TEXT_HTML)
    public Response bcidEditorAsTable(@QueryParam("ark") String identifier) {

        if (identifier == null) {
            throw new BadRequestException("You must provide an \"ark\" query parameter.");
        }

        BcidMinter d = new BcidMinter();
        String response = d.bcidEditorAsTable(username, identifier);
        d.close();
        return Response.ok(response).build();
    }

    /**
     * Service to update a Bcid's configuration.
     *
     * @param doi
     * @param webAddress
     * @param title
     * @param resourceTypeString
     * @param resourceTypesMinusDataset
     * @param stringSuffixPassThrough
     * @param identifier
     *
     * @return
     */
    @POST
    @Authenticated
    @Path("/dataGroup/update")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces(MediaType.APPLICATION_JSON)
    public Response bcidUpdate(@FormParam("doi") String doi,
                               @FormParam("webAddress") String webAddress,
                               @FormParam("title") String title,
                               @FormParam("resourceType") String resourceTypeString,
                               @FormParam("resourceTypesMinusDataset") Integer resourceTypesMinusDataset,
                               @FormParam("suffixPassThrough") String stringSuffixPassThrough,
                               @FormParam("identifier") String identifier) {
        Hashtable<String, String> config;
        Hashtable<String, String> update = new Hashtable<String, String>();

        // get this BCID's config

        BcidMinter b = new BcidMinter();
        config = b.getBcidMetadata(identifier, username.toString());

        if (resourceTypesMinusDataset != null && resourceTypesMinusDataset > 0) {
            resourceTypeString = new ResourceTypes().get(resourceTypesMinusDataset).string;
        }

        // compare every field and if they don't match, add them to the update hashtable
        if (doi != null && (!config.containsKey("doi") || !config.get("doi").equals(doi))) {
            update.put("doi", doi);
        }
        if (webAddress != null && (!config.containsKey("webAddress") || !config.get("webAddress").equals(webAddress))) {
            update.put("webAddress", webAddress);
        }
        if (!config.containsKey("title") || !config.get("title").equals(title)) {
            update.put("title", title);
        }
        if (!config.containsKey("resourceType") || !config.get("resourceType").equals(resourceTypeString)) {
            update.put("resourceTypeString", resourceTypeString);
        }
        if ((stringSuffixPassThrough != null && (stringSuffixPassThrough.equals("on") || stringSuffixPassThrough.equals("true")) && config.get("suffix").equals("false")) ||
                (stringSuffixPassThrough == null && config.get("suffix").equals("true"))) {
            if (stringSuffixPassThrough != null && (stringSuffixPassThrough.equals("on") || stringSuffixPassThrough.equals("true"))) {
                update.put("suffixPassthrough", "true");
            } else {
                update.put("suffixPassthrough", "false");
            }
        }

        if (update.isEmpty()) {
            b.close();
            return Response.ok("{\"success\": \"Nothing needed to be updated.\"}").build();
        // try to update the config by calling d.updateBcidMetadata
        } else if (b.updateBcidMetadata(update, identifier, username.toString())) {
            b.close();
            return Response.ok("{\"success\": \"BCID successfully updated.\"}").build();
        } else {
            b.close();
            // if we are here, the Bcid wasn't found
            throw new BadRequestException("Bcid wasn't found");
        }

    }
}
