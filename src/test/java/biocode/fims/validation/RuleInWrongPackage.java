package biocode.fims.validation;

import biocode.fims.models.records.RecordSet;
import biocode.fims.renderers.EntityMessages;
import biocode.fims.validation.rules.Rule;
import biocode.fims.validation.rules.RuleLevel;

/**
 * @author rjewing
 */
public class RuleInWrongPackage implements Rule {
    @Override
    public String name() {
        return "wrongPackageRule";
    }

    @Override
    public boolean run(RecordSet recordSet, EntityMessages messages) {
        return false;
    }

    @Override
    public void setColumn(String column) {
    }

    @Override
    public String column() {
        return null;
    }

    @Override
    public void setLevel(RuleLevel level) {
    }

    @Override
    public RuleLevel level() {
        return null;
    }
}
