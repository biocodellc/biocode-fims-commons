package biocode.fims.validation.rules;

import biocode.fims.models.records.GenericRecord;
import biocode.fims.models.records.Record;
import biocode.fims.models.records.RecordSet;
import biocode.fims.validation.messages.EntityMessages;
import biocode.fims.validation.messages.Message;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.Assert.*;

/**
 * @author rjewing
 */
public class UniqueValueRuleTest extends AbstractRuleTest {

    @Test(expected = IllegalArgumentException.class)
    public void should_throw_exception_for_null_recordSet() {
        Rule rule = new UniqueValueRule(null);
        assertTrue(rule.run(null, messages));
    }

    @Test
    public void should_be_valid_for_empty_recordSet() {
        Rule rule = new UniqueValueRule("col1");

        assertTrue(rule.run(new RecordSet(entity(), false), messages));
    }

    @Test
    public void should_not_validate_if_empty_columns() {
        Rule rule = new UniqueValueRule(null);

        assertFalse(rule.run(new RecordSet(entity(), false), messages));
        assertTrue(rule.hasError());

        EntityMessages expectedMessages = new EntityMessages("Samples");
        expectedMessages.addErrorMessage(
                "Invalid Rule Configuration. Contact Project Administrator.",
                new Message("Invalid UniqueValue Rule configuration. Column must not be blank or null.")
        );

        assertEquals(expectedMessages, messages);
    }

    @Test
    public void should_not_validate_if_no_attribute_for_column() {
        Rule rule = new UniqueValueRule("fake_column");

        assertFalse(rule.run(new RecordSet(entity(), false), messages));
        assertTrue(rule.hasError());

        EntityMessages expectedMessages = new EntityMessages("Samples");
        expectedMessages.addErrorMessage(
                "Invalid Rule Configuration. Contact Project Administrator.",
                new Message("Invalid UniqueValue Rule configuration. Could not find Attribute for column: fake_column in entity: Samples")
        );

        assertEquals(expectedMessages, messages);
    }

    @Test
    public void should_not_be_valid_when_duplicate_values() {
        Rule rule = new UniqueValueRule("col1", RuleLevel.ERROR);

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
                new Message("\"col1\" column is defined as unique but some values used more than once: \"" +
                        String.join("\", \"", getInvalidRecordValues().stream().distinct().collect(Collectors.toList())) + "\""
                )
        );

        assertEquals(expectedMessages, messages);
    }

    private List<Record> getValidRecords() {
        List<Record> recordSet = new ArrayList<>();

        Record r1 = new GenericRecord();
        r1.set("urn:col1", "");
        Record r2 = new GenericRecord();
        r2.set("urn:col1", "value3");
        Record r3 = new GenericRecord();
        r3.set("urn:col1", "");
        return recordSet;
    }

    private List<Record> getInvalidRecords() {
        List<Record> recordSet = new ArrayList<>();

        for (String v : getInvalidRecordValues()) {
            Record r = new GenericRecord();
            r.set("urn:col1", v);
            recordSet.add(r);
        }

        return recordSet;
    }

    private List<String> getInvalidRecordValues() {
        return Arrays.asList(
                "value1",
                "value1",
                "value2",
                "value2"
        );
    }

}