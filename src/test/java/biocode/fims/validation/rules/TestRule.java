package biocode.fims.validation.rules;

import biocode.fims.renderers.MessagesGroup;

/**
 * tmp Rule while creating the RuleTypeIdResolver
 * @author rjewing
 */
public class TestRule implements Rule {
    private static final String RULE_NAME = "test";

    private String prop1 = "property1";

    public String getProp1() {
        return prop1;
    }

    public void setProp1(String prop1) {
        this.prop1 = prop1;
    }

    @Override
    public String name() {
        return RULE_NAME;
    }

    @Override
    public void setColumn(String column) {
    }

    @Override
    public void setLevel(RuleLevel level) {
    }

    @Override
    public RuleLevel level() {
        return null;
    }

    @Override
    public MessagesGroup messages() {
        return null;
    }
}
