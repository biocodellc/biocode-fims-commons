package biocode.fims.rest.services.rest.subResources;

import biocode.fims.application.config.FimsProperties;
import biocode.fims.authorizers.QueryAuthorizer;
import biocode.fims.bcid.Identifier;
import biocode.fims.digester.Entity;
import biocode.fims.fimsExceptions.BadRequestException;
import biocode.fims.fimsExceptions.FimsRuntimeException;
import biocode.fims.fimsExceptions.ForbiddenRequestException;
import biocode.fims.fimsExceptions.errorCodes.QueryCode;
import biocode.fims.models.records.RecordResult;
import biocode.fims.projectConfig.ProjectConfig;
import biocode.fims.query.QueryResult;
import biocode.fims.repositories.ProjectConfigRepository;
import biocode.fims.repositories.RecordRepository;
import biocode.fims.rest.FimsController;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import java.util.Collections;
import java.util.Map;

/**
 * @author RJ Ewing
 */
@Controller
@Produces(MediaType.APPLICATION_JSON)
public class RecordsResource extends FimsController {
    private final RecordRepository recordRepository;
    private final ProjectConfigRepository projectConfigRepository;
    private final QueryAuthorizer queryAuthorizer;

    @Autowired
    public RecordsResource(RecordRepository recordRepository, ProjectConfigRepository projectConfigRepository,
                           QueryAuthorizer queryAuthorizer, FimsProperties props) {
        super(props);
        this.recordRepository = recordRepository;
        this.projectConfigRepository = projectConfigRepository;
        this.queryAuthorizer = queryAuthorizer;
    }

    /**
     * Get a Record by ark id
     *
     * @param identifier The ark id of the Record to fetch
     * @responseMessage 403 Invalid request. The provided ark id is missing a suffix `biocode.fims.utils.ErrorInfo
     */
    @GET
    @Path("{identifier: ark:\\/[0-9]{5}\\/.+}")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Map<String, String> get(@PathParam("identifier") String arkID) {
        Identifier identifier;
        try {
            identifier = new Identifier(arkID, props.divider());
        } catch (ArrayIndexOutOfBoundsException e) {
            throw new BadRequestException("Invalid identifier");
        }

        if (!identifier.hasSuffix()) {
            throw new BadRequestException("The provided identifier is a rootIdentifier and does not contain a suffix, therefore we can't fetch a record");
        }

        try {
            RecordResult recordResult = recordRepository.get(identifier.getRootIdentifier(), identifier.getSuffix());

            if (!queryAuthorizer.authorizedQuery(recordResult.expeditionId(), userContext.getUser())) {
                throw new ForbiddenRequestException("You are not authorized to access this record's data.");
            }

            ProjectConfig config = projectConfigRepository.getConfig(recordResult.projectId());
            Entity entity = config.entity(recordResult.conceptAlias());

            // we use QueryResult here b/c it will handle mapping column uris to column names
            return new QueryResult(
                    Collections.singletonList(recordResult.record()),
                    entity,
                    identifier.getRootIdentifier()
            ).get(false).get(0);
        } catch (FimsRuntimeException e) {
            if (e.getErrorCode() == QueryCode.NO_RESOURCES) {
                return null;
            }
            throw e;
        }
    }
}
