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
        this.config = config;
    }

    public boolean validate(RecordSet recordSet) {
        Assert.notNull(recordSet);

        if (config == null) {
            throw new IllegalStateException("ProjectConfig must not be null. Call setProjectConfig first before validate");
        }

        this.messages = new EntityMessages(recordSet.conceptAlias(), recordSet.entity().getWorksheet());

        Set<Rule> rules = recordSet.entity().getRules();
        addDefaultRules(rules, recordSet);

        for (Rule r : rules) {
            if (!r.run(recordSet, messages)) {

                if (r.hasError()) {
                    hasError = true;
                }

                isValid = false;
            }

        }
        return isValid;
    }

    private void addDefaultRules(Set<Rule> rules, RecordSet recordSet) {
        Entity entity = recordSet.entity();

        rules.add(new ValidDataTypeFormatRule());
        rules.add(new UniqueValueRule(entity.getUniqueKey(), RuleLevel.ERROR));
        rules.add(new ValidForURIRule(entity.getUniqueKey(), RuleLevel.ERROR));

        RequiredValueRule requiredValueRule = entity.getRule(RequiredValueRule.class, RuleLevel.ERROR);

        if (requiredValueRule == null) {
            requiredValueRule = new RequiredValueRule(new LinkedHashSet<>(), RuleLevel.ERROR);
            entity.addRule(requiredValueRule);
        }

        requiredValueRule.addColumn(entity.getUniqueKey());

        if (entity.isChildEntity()) {
            if (recordSet.parent() == null) {
                throw new IllegalStateException("Entity \"" + entity.getConceptAlias() + "\" is a child entity, but the RecordSet.parent() was null");
            }

            requiredValueRule.addColumn(recordSet.parent().entity().getUniqueKey());
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
