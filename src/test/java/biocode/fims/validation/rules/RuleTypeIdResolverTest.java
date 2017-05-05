package biocode.fims.validation.rules;

import biocode.fims.rest.SpringObjectMapper;
import biocode.fims.validation.RuleInWrongPackage;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.IOException;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;
import static junit.framework.TestCase.fail;

/**
 * @author rjewing
 */
public class RuleTypeIdResolverTest {

    private static ObjectMapper mapper;

    @BeforeClass
    public static void setUp() {
        mapper = new SpringObjectMapper();
    }

    @Test
    public void should_serialize_and_deserialize_rule_implementation() throws IOException {
        TestRule rule = new TestRule();
        rule.setProp1("a property value");


        String serialized = mapper.writeValueAsString(rule);
        Rule deserialized = mapper.readValue(serialized, Rule.class);

        assertTrue(deserialized instanceof TestRule);
        assertEquals("a property value", ((TestRule) deserialized).getProp1());
    }

    @Test
    public void should_throw_exception_if_rule_implementation_not_found() throws IOException {
        String ruleString = "{\"name\": \"non existent rule\"}";
        try {
            Rule rule = mapper.readValue(ruleString, Rule.class);
            fail();
        } catch (IllegalStateException e) {
            assertEquals("Could not find Rule implementation with name: \"non existent rule\" in package: biocode.fims.validation.rules", e.getMessage());
        }
    }

    @Test(expected = JsonMappingException.class)
    public void should_throw_exception_if_rule_name_missing_from_json() throws IOException {
        String ruleString = "{\"property1\": \"some property\"}";
        Rule rule = mapper.readValue(ruleString, Rule.class);
    }

    @Test
    public void should_throw_exception_if_rule_implementation_in_wrong_package() throws IOException {
        Rule rule = new RuleInWrongPackage();
        try {
            mapper.writeValueAsString(rule);
            fail();
        } catch (JsonMappingException e) {
            assertEquals("class biocode.fims.validation.RuleInWrongPackage is not in the package biocode.fims.validation.rules", e.getMessage());
        }
    }

    @Test
    public void should_throw_exception_if_RuleTypeIdResolver_used_on_non_Rule_implementation() throws IOException {
        NonRuleTypeIdResolverTestClass rule = new NonRuleTypeIdResolverTestClass();
        try {
            mapper.writeValueAsString(rule);
            fail();
        } catch (JsonMappingException e) {
            assertEquals("class biocode.fims.validation.rules.NonRuleTypeIdResolverTestClass does not implement the Rule interface", e.getMessage());
        }
    }

}