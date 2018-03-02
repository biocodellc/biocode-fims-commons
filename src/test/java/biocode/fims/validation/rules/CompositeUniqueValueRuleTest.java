package biocode.fims.validation.rules;

import biocode.fims.models.records.GenericRecord;
import biocode.fims.models.records.Record;
import biocode.fims.models.records.RecordSet;
import biocode.fims.validation.messages.EntityMessages;
import biocode.fims.validation.messages.Message;
import org.junit.Test;

import java.util.*;

import static org.junit.Assert.*;

/**
 * @author rjewing
 */
public class CompositeUniqueValueRuleTest extends AbstractRuleTest {

    @Test(expected = IllegalArgumentException.class)
    public void should_throw_exception_for_null_recordSet() {
        Rule rule = new CompositeUniqueValueRule(null);

        assertTrue(rule.run(null, messages));
    }

    @Test
    public void should_be_valid_for_empty_recordSet() {
        Rule rule = new CompositeUniqueValueRule(new LinkedHashSet(Arrays.asList("col1", "col2")));
        assertTrue(rule.run(new RecordSet(entity(), false), messages));
    }

    @Test
    public void should_not_validate_if_empty_columns() {
        Rule rule = new CompositeUniqueValueRule(new LinkedHashSet<>(Collections.emptyList()));

        assertFalse(rule.run(new RecordSet(entity(), false), messages));
        assertTrue(rule.hasError());

        EntityMessages expectedMessages = new EntityMessages("Samples");
        expectedMessages.addErrorMessage(
                "Invalid Rule Configuration. Contact Project Administrator.",
                new Message("Invalid CompositeUniqueValue Rule configuration. columns must not be empty.")
        );

        assertEquals(expectedMessages, messages);
    }

    @Test
    public void should_not_be_valid_when_duplicate_values() {
        Rule rule = new CompositeUniqueValueRule(new LinkedHashSet<>(Arrays.asList("col1", "col2")), RuleLevel.ERROR);

        RecordSet recordSet = new RecordSet(entity(), false);

        List<Record> invalidRecords = getInvalidRecords();
        invalidRecords.forEach(recordSet::add);

        List<Record> validRecords = getValidRecords();
        validRecords.forEach(recordSet::add);

        assertFalse(rule.run(recordSet, messages));
        assertTrue(rule.hasError());

        EntityMessages expectedMessages = new EntityMessages("Samples");
        expectedMessages.addErrorMessage(
                "Unique value constraint did not pass",
                new Message(
                        "(\"col1\", \"col2\") is defined as a composite unique key, but some value combinations were used " +
                                "more than once: (\"value1\", \"value2\")"
                )
        );

        assertEquals(expectedMessages, messages);
    }

    private List<Record> getValidRecords() {
        List<Record> recordSet = new ArrayList<>();

        Record r1 = new GenericRecord();
        r1.set("urn:col1", "");
        r1.set("urn:col2", "some value");
        Record r2 = new GenericRecord();
        r2.set("urn:col1", "shrimp");
        r2.set("urn:col2", "test");
        Record r3 = new GenericRecord();
        r3.set("urn:col1", "shrimp");
        r3.set("urn:col2", "some value");

        return recordSet;
    }

    private List<Record> getInvalidRecords() {
        List<Record> recordSet = new ArrayList<>();

        Record r = new GenericRecord();
        r.set("urn:col1", "value1");
        r.set("urn:col2", "value2");
        recordSet.add(r);

        Record r2 = new GenericRecord();
        r2.set("urn:col1", "value1");
        r2.set("urn:col2", "value2");
        recordSet.add(r);

        return recordSet;
    }
}