//package biocode.fims.rest.services.id;
//
//import biocode.fims.application.config.FimsProperties;
//import biocode.fims.authorizers.ProjectAuthorizer;
//import biocode.fims.bcid.BcidMetadataSchema;
//import biocode.fims.bcid.Identifier;
//import biocode.fims.bcid.Renderer.JSONRenderer;
//import biocode.fims.bcid.Renderer.RDFRenderer;
//import biocode.fims.bcid.Resolver;
//import biocode.fims.models.Expedition;
//import biocode.fims.fimsExceptions.BadRequestException;
//import biocode.fims.projectConfig.ProjectConfig;
//import biocode.fims.rest.FimsService;
//import biocode.fims.service.BcidService;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.dao.EmptyResultDataAccessException;
//import org.springframework.stereotype.Controller;
//
//import javax.ws.rs.*;
//import javax.ws.rs.core.Context;
//import javax.ws.rs.core.MediaType;
//import javax.ws.rs.core.Response;
//import javax.ws.rs.core.UriInfo;
//import java.net.URI;
//
///**
// * This is the core Resolver Service for BCIDs.  It returns URIs
// *
// * @exclude
// */
//@Controller
//@Path("/")
//public class ResolverService extends FimsService {
//
//    @Context
//    private UriInfo uriInfo;
//
//    private final BcidService bcidService;
//    private final ProjectAuthorizer projectAuthorizer;
//    private final FimsProperties props;
//    private final Resolver resolver;
//
//    @Autowired
//    ResolverService(BcidService bcidService, ProjectAuthorizer projectAuthorizer,
//                    FimsProperties props, Resolver resolver) {
//        super(props);
//        this.bcidService = bcidService;
//        this.projectAuthorizer = projectAuthorizer;
//        this.props = props;
//        this.resolver = resolver;
//    }
//
//    /**
//     * User passes in an identifier of the form scheme:/naan/shoulder_suffix
//     *
//     * @return
//     */
//    @GET
//    @Path("{identifier: .+}")
//    @Produces({MediaType.TEXT_HTML, MediaType.APPLICATION_JSON, "application/rdf+xml"})
//    public Response run(
//            @PathParam("identifier") String identifierString,
//            @HeaderParam("accept") String accept) {
//        BcidTmp bcidTmp;
//
//        String divider = props.divider();
//        Identifier identifier = new Identifier(identifierString, divider);
//        try {
//            bcidTmp = bcidService.getBcid(identifier.getBcidIdentifier());
//        } catch (EmptyResultDataAccessException e) {
//            // TODO probably want to return Viewable here
//            throw new BadRequestException("Invalid Identifier");
//        }
//
//        // When the Accept Header = "application/rdf+xml" return Metadata as RDF
//        if (accept.equalsIgnoreCase("application/rdf+xml")) {
//            // Return RDF when the Accepts header specifies rdf+xml
//            BcidMetadataSchema bcidMetadataSchema = new BcidMetadataSchema(bcidTmp, props, identifier);
//            String response = new RDFRenderer(bcidTmp, bcidMetadataSchema).render();
//            return Response.ok(response).build();
//        } else {
//            ProjectConfig config = null;
//
//            Expedition expedition = bcidTmp.getExpedition();
//
//            if (expedition != null) {
//                config = expedition.getProject().getProjectConfig();
//            }
//
//            URI resolution = resolver.resolveIdentifier(identifierString, config);
//
//            if (accept.equalsIgnoreCase("application/json"))
//                return Response.ok("{\"url\": \"" + resolution + "\"}").build();
//            else
//                return Response.seeOther(resolution).build();
//        }
//
//    }
//
//    @GET
//    @Path("metadata/{identifier: .+}")
//    @Produces(MediaType.APPLICATION_JSON)
//    public Response metadata(@PathParam("identifier") String identifierString) {
//        BcidTmp bcidTmp;
//        Identifier identifier = new Identifier(identifierString, props.divider());
//
//        try {
//            bcidTmp = bcidService.getBcid(identifier.getBcidIdentifier());
//        } catch (EmptyResultDataAccessException e) {
//            throw new BadRequestException("Invalid Identifier");
//        }
//
////        ProjectAuthorizer projectAuthorizer = new ProjectAuthorizer(projectService, props.appRoot());
//        BcidMetadataSchema bcidMetadataSchema = new BcidMetadataSchema(bcidTmp, props, identifier);
//
//        JSONRenderer renderer = new JSONRenderer(
//                userContext.getUser(),
//                bcidTmp,
//                projectAuthorizer,
//                bcidService,
//                bcidMetadataSchema,
//                props.appRoot()
//        );
//
//        return Response.ok(renderer.getMetadata()).build();
//    }
//}
