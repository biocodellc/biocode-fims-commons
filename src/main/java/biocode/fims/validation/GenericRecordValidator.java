package biocode.fims.validation;

import biocode.fims.digester.Entity;
import biocode.fims.models.records.RecordSet;
import biocode.fims.projectConfig.ProjectConfig;
import biocode.fims.renderers.EntityMessages;
import biocode.fims.validation.rules.*;
import org.springframework.util.Assert;

import java.util.*;

/**
 * @author rjewing
 */
public class GenericRecordValidator implements RecordValidator {
    private ProjectConfig config;
    private EntityMessages messages;
    private boolean hasError = false;
    private boolean isValid = true;

    @Override
    public void setProjectConfig(ProjectConfig config) {
        this.config = config;
    }

    @Override
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

    @Override
    public boolean hasError() {
        return hasError;
    }

    @Override
    public EntityMessages messages() {
        return messages;
    }
}
