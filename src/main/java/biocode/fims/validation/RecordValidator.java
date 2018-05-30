package biocode.fims.validation;

import biocode.fims.projectConfig.models.Entity;
import biocode.fims.models.records.RecordSet;
import biocode.fims.projectConfig.ProjectConfig;
import biocode.fims.validation.messages.EntityMessages;
import biocode.fims.validation.rules.*;
import org.springframework.util.Assert;

import java.util.*;

/**
 * This class serves as a base for all other RecordSet validators and is meant to be extended.
 * <p>
 * Sub-classes should also create a {@link ValidatorInstantiator} implementation to instantiate a instance. This is
 * used in the RecordValidatorFactory
 * <p>
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
        Assert.notNull(config);
        this.config = config;
    }

    public boolean validate(RecordSet recordSet) {
        Assert.notNull(recordSet);

        this.messages = new EntityMessages(recordSet.conceptAlias(), recordSet.entity().getWorksheet());

        Set<Rule> rules = recordSet.entity().getRules();
        addDefaultRules(rules, recordSet);

        for (Rule r : rules) {
            r.setProjectConfig(config);

            if (!r.run(recordSet, messages)) {

                if (r.hasError()) {
                    hasError = true;
                }

                isValid = false;
            }

        }
        return isValid;
    }

    protected void addDefaultRules(Set<Rule> rules, RecordSet recordSet) {
        Entity entity = recordSet.entity();

        rules.add(new ValidDataTypeFormatRule());
        rules.add(new ValidForURIRule(entity.getUniqueKey(), RuleLevel.ERROR));

        RequiredValueRule requiredValueRule = entity.getRule(RequiredValueRule.class, RuleLevel.ERROR);

        if (requiredValueRule == null) {
            requiredValueRule = new RequiredValueRule(new LinkedHashSet<>(), RuleLevel.ERROR);
            entity.addRule(requiredValueRule);
        }

        requiredValueRule.addColumn(entity.getUniqueKey());

        if (entity.isChildEntity()) {
            Entity parentEntity = config.entity(entity.getParentEntity());

            requiredValueRule.addColumn(parentEntity.getUniqueKey());

            LinkedHashSet<String> compositeKey = new LinkedHashSet<>(Arrays.asList(parentEntity.getUniqueKey(), entity.getUniqueKey()));
            rules.add(new CompositeUniqueValueRule(compositeKey, RuleLevel.ERROR));

            rules.add(new ValidParentIdentifiersRule());
        } else {
            rules.add(new UniqueValueRule(entity.getUniqueKey(), RuleLevel.ERROR));
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
