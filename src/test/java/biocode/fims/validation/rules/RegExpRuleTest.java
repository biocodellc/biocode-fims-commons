package biocode.fims.validation.rules;

import biocode.fims.records.GenericRecord;
import biocode.fims.records.Record;
import biocode.fims.records.RecordSet;
import biocode.fims.validation.messages.EntityMessages;
import biocode.fims.validation.messages.Message;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * @author rjewing
 */
public class RegExpRuleTest extends AbstractRuleTest {

    @Test(expected = IllegalArgumentException.class)
    public void should_throw_exception_for_null_recordSet() {
        Rule rule = new RegExpRule(null, "[a-z]");
        assertTrue(rule.run(null, messages));
    }

    @Test
    public void should_be_valid_for_empty_recordSet() {
        Rule rule = new RegExpRule("col1", "[a-z]");

        assertTrue(rule.run(new RecordSet(entity(), false), messages));
    }

    @Test
    public void should_not_validate_if_empty_column() {
        Rule rule = new RegExpRule(null, "[a-z]");

        assertFalse(rule.run(new RecordSet(entity(), false), messages));
        assertTrue(rule.hasError());

        EntityMessages expectedMessages = new EntityMessages("Samples");
        expectedMessages.addErrorMessage(
                "Invalid Rule Configuration. Contact Project Administrator.",
                new Message("Invalid RegExp Rule configuration. Column must not be blank or null.")
        );

        assertEquals(expectedMessages, messages);
    }

    @Test
    public void should_not_validate_if_empty_pattern() {
        Rule rule = new RegExpRule("col1", null);

        assertFalse(rule.run(new RecordSet(entity(), false), messages));
        assertTrue(rule.hasError());

        EntityMessages expectedMessages = new EntityMessages("Samples");
        expectedMessages.addErrorMessage(
                "Invalid Rule Configuration. Contact Project Administrator.",
                new Message("Invalid RegExp Rule configuration. pattern must not be blank or null.")
        );

        assertEquals(expectedMessages, messages);
    }

    @Test
    public void should_not_be_valid_with_case_sensitive_pattern() {

        Rule rule = new RegExpRule("col1", "[a-z]", false, RuleLevel.WARNING);

        RecordSet recordSet = new RecordSet(entity(), false);

        Record r1 = new GenericRecord();
        r1.set("urn:col1", "A");
        recordSet.add(r1);

        Record r2 = new GenericRecord();
        r2.set("urn:col1", "a");
        recordSet.add(r2);

        assertFalse(rule.run(recordSet, messages));

        EntityMessages expectedMessages = new EntityMessages("Samples");
        expectedMessages.addWarningMessage(
                "Value constraint did not pass",
                new Message("Value \"A\" in column \"col1\" does not match the pattern \"[a-z]\""
                )
        );

        assertEquals(expectedMessages, messages);
    }


    @Test
    public void should_be_valid_with_case_insensitive_pattern() {

        Rule rule = new RegExpRule("col1", "[a-z]", true, RuleLevel.WARNING);

        RecordSet recordSet = new RecordSet(entity(), false);

        Record r1 = new GenericRecord();
        r1.set("urn:col1", "A");
        recordSet.add(r1);

        Record r2 = new GenericRecord();
        r2.set("urn:col1", "a");
        recordSet.add(r2);

        assertTrue(rule.run(recordSet, messages));
    }
}