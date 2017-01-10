package biocode.fims.rest.services.id;

import biocode.fims.bcid.BcidMetadataSchema;
import biocode.fims.bcid.Identifier;
import biocode.fims.bcid.Renderer.JSONRenderer;
import biocode.fims.bcid.Renderer.RDFRenderer;
import biocode.fims.bcid.Resolver;
import biocode.fims.config.ConfigurationFileFetcher;
import biocode.fims.digester.Mapping;
import biocode.fims.entities.Bcid;
import biocode.fims.entities.Expedition;
import biocode.fims.fimsExceptions.BadRequestException;
import biocode.fims.rest.FimsService;
import biocode.fims.service.BcidService;
import biocode.fims.service.OAuthProviderService;
import biocode.fims.settings.SettingsManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.stereotype.Controller;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.File;
import java.net.URI;

/**
 * This is the core Resolver Service for BCIDs.  It returns URIs
 * @exclude
 */
@Controller
@Path("/")
public class ResolverService extends FimsService {

    private final BcidService bcidService;
    private final SettingsManager settingsManager;
    private final Resolver resolver;

    @Autowired
    ResolverService(BcidService bcidService, OAuthProviderService providerService,
                    SettingsManager settingsManager, Resolver resolver) {
        super(settingsManager);
        this.bcidService = bcidService;
        this.settingsManager = settingsManager;
        this.resolver = resolver;
    }

    /**
     * User passes in an identifier of the form scheme:/naan/shoulder_suffix
     *
     * @return
     */
    @GET
    @Path("{identifier: .+}")
    @Produces({MediaType.TEXT_HTML, MediaType.APPLICATION_JSON, "application/rdf+xml"})
    public Response run(
            @PathParam("identifier") String identifierString,
            @HeaderParam("accept") String accept) {
        Bcid bcid;

        String divider = settingsManager.retrieveValue("divider");
        Identifier identifier = new Identifier(identifierString, divider);
        try {
            bcid = bcidService.getBcid(identifier.getBcidIdentifier());
        } catch (EmptyResultDataAccessException e) {
            // TODO probably want to return Viewable here
            throw new BadRequestException("Invalid Identifier");
        }

        // When the Accept Header = "application/rdf+xml" return Metadata as RDF
        if (accept.equalsIgnoreCase("application/rdf+xml")) {
            // Return RDF when the Accepts header specifies rdf+xml
            BcidMetadataSchema bcidMetadataSchema = new BcidMetadataSchema(bcid, settingsManager, identifier);
            String response = new RDFRenderer(bcid, bcidMetadataSchema).render();
            return Response.ok(response).build();
        } else {
            Mapping mapping = null;

                Expedition expedition = bcid.getExpedition();

            if (expedition != null) {
                File configFile = new ConfigurationFileFetcher(
                        expedition.getProject().getProjectId(), uploadPath(), true
                ).getOutputFile();

                mapping = new Mapping();
                mapping.addMappingRules(configFile);
            }

            URI resolution = resolver.resolveIdentifier(identifierString, mapping);

            if (accept.equalsIgnoreCase("application/json"))
                return Response.ok("{\"url\": \"" + resolution + "\"}").build();
            else
                return Response.seeOther(resolution).build();
        }

    }

    @GET
    @Path("metadata/{identifier: .+}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response metadata (@PathParam("identifier") String identifierString) {
        Bcid bcid;
        String divider = settingsManager.retrieveValue("divider");
        Identifier identifier = new Identifier(identifierString, divider);
        String username = null;
        if (userContext.getUser() != null) {
            username = userContext.getUser().getUsername();
        }

        try {
            bcid = bcidService.getBcid(identifier.getBcidIdentifier());
        } catch (EmptyResultDataAccessException e) {
            throw new BadRequestException("Invalid Identifier");
        }

        BcidMetadataSchema bcidMetadataSchema = new BcidMetadataSchema(bcid, settingsManager, identifier);
        JSONRenderer renderer = new JSONRenderer(username, bcid, bcidMetadataSchema, settingsManager);

        return Response.ok(renderer.getMetadata().toJSONString()).build();
    }
}
