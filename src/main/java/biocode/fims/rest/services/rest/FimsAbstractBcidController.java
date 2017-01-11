package biocode.fims.rest.services.rest;

import biocode.fims.bcid.*;
import biocode.fims.bcid.Renderer.JSONRenderer;
import biocode.fims.entities.Bcid;
import biocode.fims.fimsExceptions.BadRequestException;
import biocode.fims.rest.FimsService;
import biocode.fims.rest.filters.Authenticated;
import biocode.fims.service.*;
import biocode.fims.settings.SettingsManager;
import org.apache.commons.lang.StringUtils;
import org.json.simple.JSONArray;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.Hashtable;

/**
 * REST interface calls for working with bcids.    This includes creating a bcid, looking up
 * bcids by user associated with them, and JSON representation of group metadata.
 *
 * @resourceTag Bcids
 * @exclude
 */
public abstract class FimsAbstractBcidController extends FimsService {

    private final BcidService bcidService;

    @Autowired
    FimsAbstractBcidController(BcidService bcidService, SettingsManager settingsManager) {
        super(settingsManager);
        this.bcidService = bcidService;
    }

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


        // Mint the Bcid
        if (title == null || title.isEmpty()) {
            title = resourceTypeString;
        }

        Bcid.BcidBuilder builder = new Bcid.BcidBuilder(resourceTypeString)
                .ezidRequest(Boolean.valueOf(settingsManager.retrieveValue("ezidRequests")))
                .doi(doi)
                .title(title)
                .graph(graph)
                .finalCopy(finalCopy);

        if (!StringUtils.isEmpty(webAddress)) {
            UriComponents webAddressComponents = UriComponentsBuilder.fromUriString(webAddress).build();
            builder.webAddress(webAddressComponents.toUri());
        }

        Bcid bcid = builder.build();
        bcidService.create(bcid, userContext.getUser().getUserId());

        // TODO return the bcid object here
        return Response.ok("{\"identifier\": \"" + bcid.getIdentifier() + "\"}").build();
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
        String response;
        String username = null;
        if (userContext.getUser() != null) {
            username = userContext.getUser().getUsername();
        }
        try {
            Bcid bcid = bcidService.getBcid(bcidId);
            BcidMetadataSchema bcidMetadataSchema = new BcidMetadataSchema(
                    bcid,
                    settingsManager,
                    new Identifier(
                            String.valueOf(bcid.getIdentifier()),
                            settingsManager.retrieveValue("divider")
                            )
            );

            JSONRenderer renderer = new JSONRenderer(username, bcid, bcidMetadataSchema, settingsManager);
            response = renderer.render();

        } catch (EmptyResultDataAccessException e) {
            response = "{\"Identifier\":{\"status\":\"not found\"}}";
        }

        return Response.ok(response).build();
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
        String username = null;
        if (userContext.getUser() != null) {
            username = userContext.getUser().getUsername();
        }
        JSONArray response = bcidMinter.bcidList(username);

        return Response.ok(response.toJSONString()).build();
    }

    /**
     * Service to update a Bcid's metadata
     *
     * @param doi
     * @param webAddress
     * @param title
     * @param resourceTypeString
     * @param resourceTypesMinusDataset
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
                               @FormParam("identifier") String identifier) {
        Hashtable<String, String> metadata;
        Hashtable<String, String> update = new Hashtable<String, String>();
        BcidMinter bcidMinter = new BcidMinter();

        if (identifier == null || identifier.isEmpty()) {
            throw new BadRequestException("You must include an identifier.");
        }
        if (!bcidMinter.userOwnsBcid(identifier, userContext.getUser().getUserId())) {
            throw new BadRequestException("Either the identifier doesn't exist or you are not the owner.");
        }

        // get this BCID's metadata
        metadata = bcidMinter.getBcidMetadata(identifier);

        if (resourceTypesMinusDataset != null && resourceTypesMinusDataset > 0) {
            resourceTypeString = new ResourceTypes().get(resourceTypesMinusDataset).string;
        }

        // compare every field and if they don't match, add them to the update hashtable
        if (doi != null && (!metadata.containsKey("doi") || !metadata.get("doi").equals(doi))) {
            update.put("doi", doi);
        }
        if (webAddress != null && (!metadata.containsKey("webAddress") || !metadata.get("webAddress").equals(webAddress))) {
            update.put("webAddress", webAddress);
        }
        if (title != null && (!metadata.containsKey("title") || !metadata.get("title").equals(title))) {
            update.put("title", title);
        }
        if (resourceTypeString != null && (!metadata.containsKey("resourceType") || !metadata.get("resourceType").equals(resourceTypeString))) {
            update.put("resourceTypeString", resourceTypeString);
        }

        if (update.isEmpty()) {
            return Response.ok("{\"success\": \"Nothing needed to be updated.\"}").build();
        // try to update the metadata by calling d.updateBcidMetadata
        } else if (bcidMinter.updateBcidMetadata(update, identifier)) {
            return Response.ok("{\"success\": \"BCID successfully updated.\"}").build();
        } else {
            // if we are here, the Bcid wasn't found
            throw new BadRequestException("Bcid wasn't found");
        }

    }
}
