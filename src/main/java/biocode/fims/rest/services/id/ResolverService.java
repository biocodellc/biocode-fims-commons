package biocode.fims.rest.services.id;

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

/**
 * This is the core Resolver Service for BCIDs.  It returns URIs
 */
@Path("/")
public class ResolverService extends FimsService{

    String scheme = "ark:";

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
            return Response.seeOther(r.resolveIdentifier()).build();
        }

    }

    @GET
    @Path("metadata/ark:/{naan}/{shoulderPlusIdentifier}")
    @Produces(MediaType.TEXT_HTML)
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

        Viewable v = new Viewable("/bcidMetadata.jsp", renderer.getMetadata());
        return Response.ok(v).build();
    }
}
