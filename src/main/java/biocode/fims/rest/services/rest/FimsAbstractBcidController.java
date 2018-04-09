package biocode.fims.rest.services.rest;

import biocode.fims.application.config.FimsProperties;
import biocode.fims.authorizers.ProjectAuthorizer;
import biocode.fims.bcid.*;
import biocode.fims.rest.FimsService;
import biocode.fims.rest.filters.Authenticated;
import biocode.fims.service.*;
import org.springframework.beans.factory.annotation.Autowired;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;

/**
 * REST interface calls for working with bcids.    This includes creating a bcid, looking up
 * bcids by user associated with them, and JSON representation of group metadata.
 *
 * @resourceTag Bcids
 * @exclude
 */
public abstract class FimsAbstractBcidController extends FimsService {

    private final BcidService bcidService;
    private final ProjectAuthorizer projectAuthorizer;

    @Autowired
    FimsAbstractBcidController(BcidService bcidService, FimsProperties props,
                               ProjectAuthorizer projectAuthorizer) {
        super(props);
        this.bcidService = bcidService;
        this.projectAuthorizer = projectAuthorizer;
    }

//    @Deprecated
//    @Authenticated
//    @POST
//    @Consumes(MediaType.APPLICATION_JSON)
//    @Produces(MediaType.APPLICATION_JSON)
//    public Bcid create(Bcid bcid) {
//
//        if (bcid == null) {
//            throw new javax.ws.rs.BadRequestException("bcid must not be null");
//        }
//
//        // we can override ezid requests
//        if (!props.ezidRequests()) {
//            bcid.setEzidRequest(false);
//        }
//
//        bcid.setCreator(userContext.getUser(), props.creator());
//
//        return bcidService.create(bcid, userContext.getUser());
//    }

    //TODO figure out what to do here. If were not creating dataset BCIDS, this won't work
//    /**
//     * Get the dataset BCID uploaded source file.
//     *
//     * @implicitParam identifier|string|query|true||||||the identifier of the dataset to download
//     * @excludeParams identifierString
//     *
//     * @responseType java.io.File
//     */
//    @GET
//    @Path("/dataset/{identifier: .+}")
//    public Response getSource(@PathParam("identifier") String identifierString) {
//        String divider = props.divider();
//        Identifier identifier = new Identifier(identifierString, divider);
//
//        BcidTmp bcidTmp = null;
//        try {
//            bcidTmp = bcidService.getBcid(identifier.getBcidIdentifier());
//        } catch (EmptyResultDataAccessException e) {
//            throw new BadRequestException("Invalid Identifier");
//        }
//
//        if (!bcidTmp.getResourceType().equals(ResourceTypes.DATASET_RESOURCE_TYPE)) {
//            throw new BadRequestException("BCID is not a dataset");
//        }
//
//        if (bcidTmp.getSourceFile() == null) {
//            throw new ServerErrorException("Error downloading bcid dataset.", "Bcid sourceFile is null");
//        }
//
//        if (bcidTmp.getExpedition() == null) {
//            throw new UnauthorizedRequestException("Talk to the project admin to download this bcid.");
//        }
//
//        if (!projectAuthorizer.userHasAccess(userContext.getUser(), bcidTmp.getExpedition().getProject())) {
//            throw new UnauthorizedRequestException("You are not authorized to download this private dataset.");
//        }
//
//        File file = new File(props.serverRoot() + bcidTmp.getSourceFile());
//
//        if (!file.exists()) {
//            throw new ServerErrorException("Error downloading bcid dataset.", "can't find file");
//        }
//
//        return Response.ok(file).header("Content-Disposition", "attachment; filename=" + file.getName()).build();
//    }
}
