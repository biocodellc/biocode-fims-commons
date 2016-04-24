package biocode.fims.rest.services.id;

import biocode.fims.bcid.Identifier;
import biocode.fims.bcid.Renderer.JSONRenderer;
import biocode.fims.bcid.Renderer.RDFRenderer;
import biocode.fims.bcid.Resolver;
import biocode.fims.config.ConfigurationFileFetcher;
import biocode.fims.digester.Mapping;
import biocode.fims.entities.Bcid;
import biocode.fims.entities.Expedition;
import biocode.fims.fimsExceptions.BadRequestException;
import biocode.fims.repository.BcidRepository;
import biocode.fims.repository.ExpeditionRepository;
import biocode.fims.rest.FimsService;
import biocode.fims.settings.SettingsManager;
import org.apache.commons.digester3.Digester;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.File;

/**
 * This is the core Resolver Service for BCIDs.  It returns URIs
 */
@Path("/")
public class ResolverService extends FimsService {

    private ExpeditionRepository expeditionRepository;
    private BcidRepository bcidRepository;
    private SettingsManager settingsManager;
    private Resolver resolver;

    @Autowired
    ResolverService(ExpeditionRepository expeditionRepository, BcidRepository bcidRepository,
                    SettingsManager settingsManager, Resolver resolver) {
        this.expeditionRepository = expeditionRepository;
        this.bcidRepository = bcidRepository;
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
    @Produces({MediaType.TEXT_HTML, "application/rdf+xml"})
    public Response run(
            @PathParam("identifier") String identifierString,
            @HeaderParam("accept") String accept) {
        Bcid bcid;

        String divider = settingsManager.retrieveValue("divider");
        Identifier identifier = new Identifier(identifierString, divider);
        try {
            bcid = bcidRepository.findByIdentifier(identifier.getBcidIdentifier());
        } catch (EmptyResultDataAccessException e) {
            // TODO probably want to return Viewable here
            throw new BadRequestException("Invalid Identifier");
        }

        // When the Accept Header = "application/rdf+xml" return Metadata as RDF
        if (accept.equalsIgnoreCase("application/rdf+xml")) {
            // Return RDF when the Accepts header specifies rdf+xml
            String response = new RDFRenderer(bcid).render();
            return Response.ok(response).build();
        } else {
            Mapping mapping = null;

                Expedition expedition = expeditionRepository.findByBcid(bcid);

            if (expedition != null) {
                File configFile = new ConfigurationFileFetcher(
                        expedition.getProjectId(), uploadPath(), false
                ).getOutputFile();

                mapping = new Mapping();
                mapping.addMappingRules(new Digester(), configFile);
            }

            return Response.seeOther(resolver.resolveIdentifier(identifierString, mapping)).build();
        }

    }

    @GET
    @Path("metadata/{identifier: .+}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response metadata (@PathParam("identifier") String identifierString) {
        Bcid bcid;
        String divider = settingsManager.retrieveValue("divider");
        Identifier identifier = new Identifier(identifierString, divider);

        try {
            bcid = bcidRepository.findByIdentifier(identifier.getBcidIdentifier());
        } catch (EmptyResultDataAccessException e) {
            throw new BadRequestException("Invalid Identifier");
        }

        JSONRenderer renderer = new JSONRenderer(username, bcid, expeditionRepository, settingsManager);

        return Response.ok(renderer.getMetadata().toJSONString()).build();
    }
}
