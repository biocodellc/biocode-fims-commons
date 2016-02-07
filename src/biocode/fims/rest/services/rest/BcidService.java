package biocode.fims.rest.services.rest;

import biocode.fims.bcid.*;
import biocode.fims.bcid.Renderer.JSONRenderer;
import biocode.fims.bcid.Renderer.Renderer;
import biocode.fims.fimsExceptions.BadRequestException;
import biocode.fims.rest.FimsService;
import biocode.fims.rest.filters.Authenticated;
import org.json.simple.JSONArray;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.Hashtable;

/**
 * REST interface calls for working with bcids.    This includes creating a bcid, looking up
 * bcids by user associated with them, and JSON representation of group metadata.
 */
@Path("bcids")
public class BcidService extends FimsService {
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
                         @FormParam("suffixPassThrough") @DefaultValue("false") Boolean suffixPassThrough,
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

        if (suffixPassThrough && (webAddress == null || webAddress.isEmpty())) {
            throw new BadRequestException("You must provide a Target URL if following suffixes.");
        }

        // Mint the Bcid
        BcidMinter bcidMinter = new BcidMinter(Boolean.valueOf(sm.retrieveValue("ezidRequests")));
        if (title == null || title.isEmpty()) {
            title = resourceTypeString;
        }
        String identifier = bcidMinter.createEntityBcid(new Bcid(userId, resourceTypeString, title,
                webAddress, graph, doi, finalCopy, suffixPassThrough));
        bcidMinter.close();

        return Response.ok("{\"identifier\": \"" + identifier + "\"}").build();
    }

    /**
     * Return a JSON representation of bcids metadata
     *
     *
     * @param bcidId
     * @return
     */
    @GET
    @Path("/metadata/{bcidId}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response run(@PathParam("bcidId") Integer bcidId) {
        Bcid bcid = new Bcid(bcidId);
        Resolver resolver = new Resolver(bcid.getIdentifier().toString());
        Renderer renderer = new JSONRenderer(username, resolver);

        return Response.ok(renderer.render(bcid)).build();
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
        BcidMinter bcidMinter = new BcidMinter();
        JSONArray response = bcidMinter.bcidList(username);
        bcidMinter.close();

        return Response.ok(response.toJSONString()).build();
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
    @Path("/update")
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

        BcidMinter bcidMinter = new BcidMinter();
        config = bcidMinter.getBcidMetadata(identifier, username);

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
            bcidMinter.close();
            return Response.ok("{\"success\": \"Nothing needed to be updated.\"}").build();
        // try to update the config by calling d.updateBcidMetadata
        } else if (bcidMinter.updateBcidMetadata(update, identifier, username.toString())) {
            bcidMinter.close();
            return Response.ok("{\"success\": \"BCID successfully updated.\"}").build();
        } else {
            bcidMinter.close();
            // if we are here, the Bcid wasn't found
            throw new BadRequestException("Bcid wasn't found");
        }

    }
}
