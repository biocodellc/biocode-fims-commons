package biocode.fims.validation;

import biocode.fims.projectConfig.models.Entity;
import biocode.fims.fimsExceptions.FimsRuntimeException;
import biocode.fims.fimsExceptions.errorCodes.DataReaderCode;
import biocode.fims.models.records.RecordSet;
import biocode.fims.projectConfig.ProjectConfig;
import biocode.fims.validation.messages.EntityMessages;
import biocode.fims.validation.messages.Message;
import biocode.fims.run.Dataset;
import biocode.fims.run.ProcessorStatus;

import java.util.*;

/**
 * Runs the appropriate {@link RecordValidator} on a group of {@link RecordSet}s.
 * <p>
 * Any parent {@link RecordSet} which are on a multi-entity worksheet, will be de-duplicated before the
 * {@link RecordValidator}s are run.
 *
 * @author rjewing
 */
public class DatasetValidator {
    private final RecordValidatorFactory validatorFactory;
    private final Dataset dataset;
    private final ProjectConfig config;

    private final LinkedList<EntityMessages> messages;
    private final Map<Entity, Message> removeDuplicateMessages;
    private boolean hasError = false;

    public DatasetValidator(RecordValidatorFactory validatorFactory, Dataset dataset, ProjectConfig config) {
        this.validatorFactory = validatorFactory;
        this.dataset = dataset;
        this.config = config;
        this.messages = new LinkedList<>();
        this.removeDuplicateMessages = new HashMap<>();
    }

    public boolean validate(ProcessorStatus processorStatus) {
        boolean isValid = true;

        removeDuplicateParentRecords();

        for (RecordSet r : dataset) {

            processorStatus.appendStatus("\nValidating entity: " + r.conceptAlias());

            RecordValidator validator = validatorFactory.getValidator(r.entity().getRecordType(), config);

            if (!validator.validate(r)) {

                isValid = false;
                if (validator.hasError()) {
                    hasError = true;
                }

                messages.add(validator.messages());
            }

        }

        addRemoveDuplicateRecordMessages();

        return !hasError && isValid;
    }

    private void removeDuplicateParentRecords() {
        for (RecordSet r : dataset) {

            Entity entity = r.entity();

            if (entity.isChildEntity() && config.isMultiSheetEntity(entity.getConceptAlias())) {
                Entity parentEntity = r.parent().entity();
                try {
                    r.parent().removeDuplicates();
                } catch (FimsRuntimeException e) {
                    if (e.getErrorCode().equals(DataReaderCode.INVALID_RECORDS)) {
                        hasError = true;

                        removeDuplicateMessages.put(
                                parentEntity,
                                new Message("Duplicate \"" + parentEntity.getUniqueKey() + "\" values, however the other columns are not the same.")
                        );
                    }

                }
            }
        }

    }

    private void addRemoveDuplicateRecordMessages() {
        for (Map.Entry<Entity, Message> entry : removeDuplicateMessages.entrySet()) {
            Entity entity = entry.getKey();

            EntityMessages entityMessages = null;
            for (EntityMessages em : messages) {
                if (em.conceptAlias().equals(entity.getConceptAlias())) {
                    entityMessages = em;
                    break;
                }
            }

            if (entityMessages == null) {
                entityMessages = new EntityMessages(entity.getConceptAlias(), entity.getWorksheet());
                messages.add(entityMessages);
            }

            entityMessages.addErrorMessage(
                    "Duplicate parent records",
                    entry.getValue()
            );
        }
    }

    public boolean hasError() {
        return hasError;
    }

    public List<EntityMessages> messages() {
        return messages;
    }
}
