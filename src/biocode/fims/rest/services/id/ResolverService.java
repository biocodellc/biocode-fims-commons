package biocode.fims.rest.services.id;

import biocode.fims.bcid.Bcid;
import biocode.fims.bcid.BcidMetadataSchema;
import biocode.fims.bcid.Renderer.JSONRenderer;
import biocode.fims.bcid.Renderer.RDFRenderer;
import biocode.fims.bcid.Resolver;
import biocode.fims.fimsExceptions.BadRequestException;
import biocode.fims.rest.FimsService;
import org.glassfish.jersey.server.mvc.Viewable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;

/**
 * This is the core Resolver Service for BCIDs.  It returns URIs
 */
@Path("ark:")
public class ResolverService extends FimsService{

    String scheme = "ark:";

    private static Logger logger = LoggerFactory.getLogger(ResolverService.class);

    /**
     * User passes in an Bcid of the form scheme:/naan/shoulder_identifier
     *
     * @param naan
     * @param shoulderPlusIdentifier
     *
     * @return
     */
    @GET
    @Path("/{naan}/{shoulderPlusIdentifier}")
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

        try {
            if (accept.equalsIgnoreCase("application/rdf+xml")) {
                // Return RDF when the Accepts header specifies rdf+xml
                String response = new RDFRenderer(r.getBcid()).render();
                return Response.ok(response).build();
            } else if (r.forwardBCID()) {
                return Response.seeOther(r.getResolutionTarget()).build();

            } else {

                // This next section uses the Jersey Viewable class, which is a type of Model, View, Controller
                // construct, enabling us to pass content JSP code to a JSP template.  We do this in this section
                // so we can have a REST style call and provide human readable content with BCID header/footer
//                Map<String, Object> map = new HashMap<String, Object>();
//                String response = r.printMetadata(new HTMLTableRenderer());
//                BcidMetadataSchema.metadataElement metadataElement = new BcidMetadataSchema.metadataElement()
//                map.put("metadata", r.getMetadata());
                JSONRenderer renderer = new JSONRenderer(username, r, r.getBcid());

                Viewable v = new Viewable("/bcidMetadata.jsp", renderer.getMetadata());
                return Response.ok(v).build();
            }


//            } catch (URISyntaxException e) {
//                logger.warn("URISyntaxException while trying to resolve ARK for element: {}", element, e);
//                throw new BadRequestException("Server error while trying to resolve ARK. Did you supply a valid naan?");
//            }

            // The expected response for IDentifiers without a URL
//            return Response.ok("{\"url\": \"" + seeOtherUri + "\"}").build();
        } finally {
            r.close();
        }

    }
}
