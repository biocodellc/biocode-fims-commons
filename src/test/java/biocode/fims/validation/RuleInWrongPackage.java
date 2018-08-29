package biocode.fims.validation;

import biocode.fims.config.models.Entity;
import biocode.fims.records.RecordSet;
import biocode.fims.config.project.ProjectConfig;
import biocode.fims.validation.messages.EntityMessages;
import biocode.fims.validation.rules.Rule;
import biocode.fims.validation.rules.RuleLevel;

import java.util.List;

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
    public RuleLevel level() {
        return null;
    }

    @Override
    public boolean validConfiguration(List<String> messages, Entity entity) {
        return false;
    }

    @Override
    public boolean hasError() {
        return false;
    }

    @Override
    public boolean isNetworkRule() {
        return false;
    }

    @Override
    public void setNetworkRule(boolean isNetworkRule) {
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
    public void setProjectConfig(ProjectConfig config) {
    }
}
