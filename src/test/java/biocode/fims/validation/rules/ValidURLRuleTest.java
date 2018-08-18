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
public class ValidURLRuleTest extends AbstractRuleTest {

    @Test(expected = IllegalArgumentException.class)
    public void should_throw_exception_for_null_recordSet() {
        Rule rule = new ValidURLRule(null);
        assertTrue(rule.run(null, messages));
    }

    @Test
    public void should_be_valid_for_empty_recordSet() {
        Rule rule = new ValidURLRule("col1");

        assertTrue(rule.run(new RecordSet(entity(), false), messages));
    }

    @Test
    public void should_not_validate_if_empty_columns() {
        Rule rule = new ValidURLRule(null);

        assertFalse(rule.run(new RecordSet(entity(), false), messages));
        assertTrue(rule.hasError());

        EntityMessages expectedMessages = new EntityMessages("Samples");
        expectedMessages.addErrorMessage(
                "Invalid Rule Configuration. Contact Project Administrator.",
                new Message("Invalid ValidURL Rule configuration. Column must not be blank or null.")
        );

        assertEquals(expectedMessages, messages);
    }

    @Test
    public void should_not_be_valid_when_invalid_url_values() {
        Rule rule = new ValidURLRule("col1", RuleLevel.ERROR);

        RecordSet recordSet = new RecordSet(entity(), false);

        Record r1 = new GenericRecord();
        r1.set("urn:col1", "http://example.com");
        recordSet.add(r1);

        Record r2 = new GenericRecord();
        r2.set("urn:col1", "http:/example.com");
        recordSet.add(r2);

        Record r3 = new GenericRecord();
        r3.set("urn:col1", "example.com");
        recordSet.add(r3);

        assertFalse(rule.run(recordSet, messages));
        assertTrue(rule.hasError());

        EntityMessages expectedMessages = new EntityMessages("Samples");
        expectedMessages.addErrorMessage(
                "Invalid URL",
                new Message("\"http:/example.com\" is not a valid URL for \"col1\"")
        );
        expectedMessages.addErrorMessage(
                "Invalid URL",
                new Message("\"example.com\" is not a valid URL for \"col1\"")
        );

        assertEquals(expectedMessages, messages);
    }
}