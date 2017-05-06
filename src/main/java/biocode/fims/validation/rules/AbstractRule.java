package biocode.fims.validation.rules;

import biocode.fims.digester.Entity;
import biocode.fims.fimsExceptions.FimsRuntimeException;
import biocode.fims.fimsExceptions.errorCodes.ConfigCode;
import biocode.fims.models.records.RecordSet;
import biocode.fims.renderers.EntityMessages;
import biocode.fims.renderers.SimpleMessage;

import java.util.ArrayList;
import java.util.List;

/**
 * @author rjewing
 */
abstract class AbstractRule implements Rule {
    private boolean hasError = false;
    private RuleLevel level;

    AbstractRule() {}

    AbstractRule(RuleLevel level) {
        this.level = level;
    }

    public RuleLevel level() {
        return level;
    }

    public boolean hasError() {
        return hasError;
    }

    void setError() {
        hasError = RuleLevel.ERROR == level;
    }

    boolean entityHasAttribute(List<String> messages, Entity entity, String column) {
        try {
            entity.getAttribute(column);
        } catch (FimsRuntimeException e) {
            if (e.getErrorCode() == ConfigCode.MISSING_ATTRIBUTE) {
                messages.add(
                        "Invalid " + name() + " Rule configuration. Could not find Attribute for column: " + column + " in entity: " + entity.getConceptAlias()
                );

                return false;
            } else {
                throw e;
            }
        }

        return true;
    }

    boolean validConfiguration(RecordSet recordSet, EntityMessages messages) {
        List<String> configMessages = new ArrayList<>();

        if (!validConfiguration(configMessages, recordSet.entity())) {
            for (String msg: configMessages) {
                messages.addErrorMessage(
                        "Invalid Rule Configuration. Contact Project Administrator.",
                        new SimpleMessage(msg)
                );
            }

            hasError = true;
            return false;

        }

        return true;
    }
}
