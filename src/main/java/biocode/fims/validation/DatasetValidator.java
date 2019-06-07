package biocode.fims.validation;

import biocode.fims.config.models.Entity;
import biocode.fims.config.project.ProjectConfig;
import biocode.fims.fimsExceptions.FimsRuntimeException;
import biocode.fims.fimsExceptions.errorCodes.DataReaderCode;
import biocode.fims.records.RecordSet;
import biocode.fims.validation.messages.EntityMessages;
import biocode.fims.validation.messages.Message;
import biocode.fims.run.Dataset;
import biocode.fims.run.ProcessorStatus;
import biocode.fims.validation.rules.RuleLevel;
import org.apache.commons.collections.keyvalue.MultiKey;

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

    private LinkedList<EntityMessages> messages;
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

            // skip recordSet validation if there are not records to persist
            if (!r.hasRecordToPersist()) continue;

            processorStatus.appendStatus("\nValidating entity: " + r.conceptAlias());

            RecordValidator validator = validatorFactory.getValidator(r.entity().getRecordType(), config);

            if (!validator.validate(r)) {

                isValid = false;
                if (validator.hasError()) {
                    hasError = true;
                }

                messages.add(validator.messages());
            }

            if (r.expeditionCode() == null) {
                EntityMessages entityMessages = messages.stream()
                        .filter(e -> e.conceptAlias().equals(r.conceptAlias()))
                        .findFirst()
                        .orElseGet(() -> {
                            EntityMessages em = new EntityMessages(r.conceptAlias(), r.entity().getWorksheet());
                            messages.add(em);
                            return em;
                        });

                entityMessages.addErrorMessage(
                        "Missing expeditionCode",
                        new Message("One or more records are missing an expeditionCode. " +
                                "When uploading data from multiple expeditions, each record in the " +
                                r.conceptAlias() + "worksheet must have a pre-existing expeditionCode " +
                                "specified for all records (in the column \"expeditionCode\"). You can obtain " +
                                "a list of all expedition codes for your project by first downloading a workbook " +
                                "generated for your project, either through the \"Project Overview\" page " +
                                "or from the \"Query\" page, and updating records. Note: All expedition codes in " +
                                "the worksheet MUST EXIST.")
                );

                isValid = false;
                hasError = true;
            }

        }

        addRemoveDuplicateRecordMessages();
        mergeMessages();

        return !hasError && isValid;
    }

    /**
     * Multi-expedition uploads will crate duplicate EntityMessage objects, so we merge them
     */
    private void mergeMessages() {
        Map<MultiKey, EntityMessages> newMessages = new HashMap<>();

        messages.forEach(m -> {
            MultiKey k = new MultiKey(m.conceptAlias(), m.sheetName());

            if (newMessages.containsKey(k)) {
                EntityMessages em = newMessages.get(k);

                m.warningMessages().forEach(wm -> wm.messages()
                        .forEach(message -> em.addWarningMessage(wm.getName(), message)));
                m.errorMessages().forEach(wm -> wm.messages()
                        .forEach(message -> em.addErrorMessage(wm.getName(), message)));

            } else {
                newMessages.put(k, m);
            }
        });

        messages = new LinkedList<>(newMessages.values());
    }

    private void removeDuplicateParentRecords() {
        for (RecordSet r : dataset) {

            Entity entity = r.entity();

            if (entity.isChildEntity() && config.isMultiSheetEntity(entity.getConceptAlias()) && r.hasParent()) {
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
