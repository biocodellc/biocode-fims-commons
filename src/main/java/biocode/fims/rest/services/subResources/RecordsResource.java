package biocode.fims.rest.services.subResources;

import biocode.fims.application.config.FimsProperties;
import biocode.fims.fimsExceptions.*;
import biocode.fims.fimsExceptions.BadRequestException;
import biocode.fims.fimsExceptions.errorCodes.QueryCode;
import biocode.fims.rest.FimsController;
import biocode.fims.rest.responses.ConfirmationResponse;
import biocode.fims.rest.responses.RecordResponse;
import biocode.fims.service.RecordService;
import biocode.fims.utils.Flag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;

/**
 * @author RJ Ewing
 */
@Controller
@Produces(MediaType.APPLICATION_JSON)
public class RecordsResource extends FimsController {
    private final RecordService recordService;

    @Autowired
    public RecordsResource(RecordService recordService, FimsProperties props) {
        super(props);
        this.recordService = recordService;
    }

    /**
     * Get a Record by ark id
     *
     * @param identifier The ark id of the Record to fetch
     * @responseMessage 400 Invalid request. The provided ark id is missing a suffix `biocode.fims.utils.ErrorInfo
     */
    @GET
    @Path("{identifier: ark:\\/[0-9]{5}\\/.+}")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public RecordResponse get(@QueryParam("includeChildren") @DefaultValue("false") Flag includeChildren,
                              @QueryParam("includeParent") @DefaultValue("false") Flag includeParent,
                              @PathParam("identifier") String arkID) {

        try {
            return recordService.get(userContext.getUser(), arkID, includeParent.isPresent(), includeChildren.isPresent());
        } catch (FimsRuntimeException e) {
            if (e.getErrorCode() == QueryCode.NO_RESOURCES) {
                return null;
            }
            throw e;
        }
    }

    /**
     * Delete a Record by ark id
     *
     * @param identifier The ark id of the Record to fetch
     * @responseMessage 400 Invalid request. The provided ark id is missing a suffix `biocode.fims.utils.ErrorInfo
     */
    @DELETE
    @Path("{identifier: ark:\\/[0-9]{5}\\/.+}")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public ConfirmationResponse delete(@PathParam("identifier") String arkID) {
        try {
            return new ConfirmationResponse(recordService.delete(userContext.getUser(), arkID));
        } catch (FimsRuntimeException e) {
            if (e.getErrorCode() == QueryCode.NO_RESOURCES) {
                throw new BadRequestException("Records doesn't exist", e);
            }
            throw e;
        }
    }
}
