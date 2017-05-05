package biocode.fims.validation;

import biocode.fims.validation.rules.Rule;

/**
 * @author rjewing
 */
public class RuleInWrongPackage implements Rule {
    @Override
    public String name() {
        return "wrongPackageRule";
    }
}
