package biocode.fims.validation;

import biocode.fims.digester.Entity;
import biocode.fims.models.records.RecordSet;
import biocode.fims.projectConfig.ProjectConfig;
import biocode.fims.renderers.EntityMessages;
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
}
