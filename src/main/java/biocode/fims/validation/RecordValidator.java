package biocode.fims.validation;

import biocode.fims.digester.Entity;
import biocode.fims.models.records.RecordSet;
import biocode.fims.projectConfig.ProjectConfig;
import biocode.fims.renderers.EntityMessages;
import biocode.fims.validation.rules.*;
import org.springframework.util.Assert;

import java.util.*;

/**
 * This class serves as a base for all other RecordSet validators and is meant to be extended.
 *
 * Sub-classes should also create a {@link ValidatorInstantiator} implementation to instantiate a instance. This is
 * used in the RecordValidatorFactory
 *
 * This class will apply any {@link Rule} defined in the {@link Entity} to the {@link RecordSet}
 *
 * @author rjewing
 */
public class RecordValidator {
    protected ProjectConfig config;
    protected EntityMessages messages;
    protected boolean hasError = false;
    protected boolean isValid = true;

    public RecordValidator(ProjectConfig config) {
        this.config = config;
    }

    public boolean validate(RecordSet recordSet) {
        Assert.notNull(recordSet);

        if (config == null) {
            throw new IllegalStateException("ProjectConfig must not be null. Call setProjectConfig first before validate");
        }

        this.messages = new EntityMessages(recordSet.conceptAlias(), recordSet.entity().getWorksheet());

        Set<Rule> rules = recordSet.entity().getRules();
        addDefaultRules(rules, recordSet.entity());

        for (Rule r: rules) {
            if (!r.run(recordSet, messages)) {

                if (r.hasError()) {
                    hasError = true;
                }

                isValid = false;
            }

        }
        return isValid;
    }

    private void addDefaultRules(Set<Rule> rules, Entity entity) {
        rules.add(new ValidDataTypeFormatRule());
        rules.add(new UniqueValueRule(entity.getUniqueKey(), RuleLevel.ERROR));
        rules.add(new ValidForURIRule(entity.getUniqueKey(), RuleLevel.ERROR));

        boolean setRequiredValueRule = false;
        for (Rule rule: rules) {
            if (rule instanceof RequiredValueRule && rule.level() == RuleLevel.ERROR) {
                ((RequiredValueRule) rule).addColumn(entity.getUniqueKey());
                setRequiredValueRule = true;
                break;
            }
        }

        if (!setRequiredValueRule) {
            rules.add(new RequiredValueRule(new LinkedHashSet<>(Collections.singletonList(entity.getUniqueKey())), RuleLevel.ERROR));
        }
    }

    public boolean hasError() {
        return hasError;
    }

    public EntityMessages messages() {
        return messages;
    }

    public static class DefaultValidatorInstantiator implements ValidatorInstantiator {
        @Override
        public RecordValidator newInstance(ProjectConfig config) {
            return new RecordValidator(config);
        }
    }
}
