package biocode.fims.rest.services.id;

import biocode.fims.bcid.Bcid;
import biocode.fims.bcid.Renderer.JSONRenderer;
import biocode.fims.bcid.Renderer.RDFRenderer;
import biocode.fims.bcid.Resolver;
import biocode.fims.config.ConfigurationFileFetcher;
import biocode.fims.digester.Mapping;
import biocode.fims.entities.Expedition;
import biocode.fims.fimsExceptions.BadRequestException;
import biocode.fims.repository.ExpeditionRepository;
import biocode.fims.rest.FimsService;
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

    String scheme = "ark:";

    @Autowired
    ExpeditionRepository expeditionRepository;

    /**
     * User passes in an Bcid of the form scheme:/naan/shoulder_identifier
     *
     * @param naan
     * @param shoulderPlusIdentifier
     *
     * @return
     */
    @GET
    @Path("ark:/{naan}/{shoulderPlusIdentifier}")
    @Produces({MediaType.TEXT_HTML, "application/rdf+xml"})
    public Response run(
            @PathParam("naan") String naan,
            @PathParam("shoulderPlusIdentifier") String shoulderPlusIdentifier,
            @HeaderParam("accept") String accept) {

        shoulderPlusIdentifier = shoulderPlusIdentifier.trim();

        // Structure the Bcid element from path parameters
        String element = scheme + "/" + naan + "/" + shoulderPlusIdentifier;

        // When the Accept Header = "application/rdf+xml" return Metadata as RDF
        Resolver r;
        try {
            r = new Resolver(element);
        } catch (BadRequestException e) {
            // TODO probably want to return Viewable here
            throw e;
        }

        if (accept.equalsIgnoreCase("application/rdf+xml")) {
            // Return RDF when the Accepts header specifies rdf+xml
            String response = new RDFRenderer(r.getBcid()).render();
            return Response.ok(response).build();
        } else {
            Mapping mapping = null;

            try {
                Expedition expedition = expeditionRepository.findByBcid(r.getBcid());

                File configFile = new ConfigurationFileFetcher(
                        expedition.getProjectId(), uploadPath(), false
                ).getOutputFile();

                mapping = new Mapping();
                mapping.addMappingRules(new Digester(), configFile);
            } catch (EmptyResultDataAccessException e) {
                // do nothing as not every Bcid is associated with an Expedition
            }

            return Response.seeOther(r.resolveIdentifier(mapping)).build();
        }

    }

    @GET
    @Path("metadata/ark:/{naan}/{shoulderPlusIdentifier}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response metadata (
            @PathParam("naan") String naan,
            @PathParam("shoulderPlusIdentifier") String shoulderPlusIdentifier) {
        shoulderPlusIdentifier = shoulderPlusIdentifier.trim();

        // Structure the Bcid element from path parameters
        String element = scheme + "/" + naan + "/" + shoulderPlusIdentifier;

        Resolver resolver = new Resolver(element);

        // This next section uses the Jersey Viewable class, which is a type of Model, View, Controller
        // construct, enabling us to pass content JSP code to a JSP template.  We do this in this section
        // so we can have a REST style call and provide human readable content with BCID header/footer
        JSONRenderer renderer = new JSONRenderer(username, resolver, resolver.getBcid());

        return Response.ok(renderer.getMetadata().toJSONString()).build();
    }
}
