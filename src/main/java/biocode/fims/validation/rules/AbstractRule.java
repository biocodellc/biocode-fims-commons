package biocode.fims.validation.rules;

import biocode.fims.config.Config;
import biocode.fims.config.models.Entity;
import biocode.fims.config.project.ProjectConfig;
import biocode.fims.fimsExceptions.FimsRuntimeException;
import biocode.fims.fimsExceptions.errorCodes.ConfigCode;
import biocode.fims.records.RecordSet;
import biocode.fims.validation.messages.EntityMessages;
import biocode.fims.validation.messages.Message;
import org.springframework.util.Assert;

import java.util.ArrayList;
import java.util.List;

/**
 * @author rjewing
 */
public abstract class AbstractRule implements Rule {
    private boolean hasError = false;
    private RuleLevel level;
    protected boolean networkRule = false;
    protected Config config;

    AbstractRule() {
        this.level = RuleLevel.WARNING;
    }

    AbstractRule(RuleLevel level) {
        this.level = level;
    }

    @Override
    public RuleLevel level() {
        return level;
    }

    @Override
    public boolean hasError() {
        return hasError;
    }

    void setError() {
        hasError = RuleLevel.ERROR == level;
    }

    @Override
    public void setProjectConfig(ProjectConfig config) {
        Assert.notNull(config);
        this.config = config;
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
                        new Message(msg)
                );
            }

            hasError = true;
            return false;

        }

        return true;
    }

    @Override
    public boolean isNetworkRule() {
        return networkRule;
    }

    @Override
    public void setNetworkRule(boolean networkRule) {
        this.networkRule = networkRule;
    }

    @Override
    public boolean mergeRule(Rule r) {
        return false;
    }

    @Override
    public boolean contains(Rule r) {
        return false;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof AbstractRule)) return false;

        AbstractRule that = (AbstractRule) o;
        if (name() != null ? !name().equals(that.name()) : that.name() != null) {
            return false;
        }

        return level == that.level;
    }

    @Override
    public int hashCode() {
        int result = level != null ? level.hashCode() : 0;
        result = 31 * result + (name() != null ? name().hashCode() : 0);
        return result;
    }
}
