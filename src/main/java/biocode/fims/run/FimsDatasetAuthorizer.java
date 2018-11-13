package biocode.fims.run;

import biocode.fims.application.config.FimsProperties;
import biocode.fims.fimsExceptions.FimsRuntimeException;
import biocode.fims.fimsExceptions.errorCodes.UploadCode;
import biocode.fims.models.EntityIdentifier;
import biocode.fims.models.Expedition;
import biocode.fims.models.Project;
import biocode.fims.models.User;
import biocode.fims.records.RecordSet;
import biocode.fims.service.ExpeditionService;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * @author rjewing
 */
public class FimsDatasetAuthorizer implements DatasetAuthorizer {

    private final FimsProperties props;
    private final ExpeditionService expeditionService;

    @Autowired
    public FimsDatasetAuthorizer(FimsProperties props, ExpeditionService expeditionService) {
        this.props = props;
        this.expeditionService = expeditionService;
    }

    @Override
    public boolean authorize(Dataset dataset, Project project, User user) {
        checkNullUser(user);

        // projectAdmin can modify expedition data
        boolean ignoreUser = props.ignoreUser() || project.getUser().equals(user);

        List<String> expeditionCodes = dataset.stream()
                .filter(RecordSet::hasRecordToPersist)
                .map(RecordSet::expeditionCode)
                .distinct()
                .collect(Collectors.toList());

        List<Expedition> expeditions = expeditionService.getExpeditions(project.getProjectId(), true);

        for (String expeditionCode : expeditionCodes) {
            Optional<Expedition> expedition = expeditions.stream().filter(e -> e.getExpeditionCode().equals(expeditionCode)).findFirst();

            if (!expedition.isPresent()) {
                throw new FimsRuntimeException(UploadCode.INVALID_EXPEDITION, 400, expeditionCode);
            } else if (!ignoreUser) {
                if (!expedition.get().getUser().equals(user)) {
                    throw new FimsRuntimeException(UploadCode.USER_NO_OWN_EXPEDITION, 400, expeditionCode);
                }
            }
        }

        return true;
    }

    @Override
    public boolean authorize(EntityIdentifier entityIdentifier, User user) {
        checkNullUser(user);

        // projectAdmin can modify expedition data
        boolean ignoreUser = props.ignoreUser() || entityIdentifier.getExpedition().getProject().getUser().equals(user);

        if (!ignoreUser && !entityIdentifier.getExpedition().getUser().equals(user)) {
            throw new FimsRuntimeException(UploadCode.USER_NO_OWN_EXPEDITION, 400, entityIdentifier.getExpedition().getExpeditionCode());
        }

        return true;
    }

    private void checkNullUser(User user) {
        if (user == null) {
            throw new FimsRuntimeException("you must be logged in to upload", 400);
        }
    }
}
