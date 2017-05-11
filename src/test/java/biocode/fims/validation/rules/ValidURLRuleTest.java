package biocode.fims.validation.rules;

import biocode.fims.models.records.GenericRecord;
import biocode.fims.models.records.Record;
import biocode.fims.models.records.RecordSet;
import biocode.fims.renderers.EntityMessages;
import biocode.fims.renderers.SimpleMessage;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

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

        assertTrue(rule.run(new RecordSet(entity()), messages));
    }

    @Test
    public void should_not_validate_if_empty_columns() {
        Rule rule = new ValidURLRule(null);

        assertFalse(rule.run(new RecordSet(entity()), messages));
        assertTrue(rule.hasError());

        EntityMessages expectedMessages = new EntityMessages("Samples");
        expectedMessages.addErrorMessage(
                "Invalid Rule Configuration. Contact Project Administrator.",
                new SimpleMessage("Invalid ValidURL Rule configuration. Column must not be blank or null.")
        );

        assertEquals(expectedMessages, messages);
    }

    @Test
    public void should_not_be_valid_when_invalid_url_values() {
        Rule rule = new ValidURLRule("col1", RuleLevel.ERROR);

        RecordSet recordSet = new RecordSet(entity());

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
                new SimpleMessage("\"http:/example.com\" is not a valid URL for \"col1\"")
        );
        expectedMessages.addErrorMessage(
                "Invalid URL",
                new SimpleMessage("\"example.com\" is not a valid URL for \"col1\"")
        );

        assertEquals(expectedMessages, messages);
    }
}