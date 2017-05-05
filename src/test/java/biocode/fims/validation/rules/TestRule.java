package biocode.fims.validation.rules;

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
}
