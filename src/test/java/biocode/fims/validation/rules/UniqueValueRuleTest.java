package biocode.fims.validation.rules;

import biocode.fims.models.records.GenericRecord;
import biocode.fims.models.records.Record;
import biocode.fims.models.records.RecordSet;
import biocode.fims.projectConfig.models.Entity;
import biocode.fims.validation.messages.EntityMessages;
import biocode.fims.validation.messages.Message;
import org.junit.Test;

import java.util.*;
import java.util.stream.Collectors;

import static org.junit.Assert.*;

/**
 * @author rjewing
 */
public class UniqueValueRuleTest extends AbstractRuleTest {

    @Test(expected = IllegalArgumentException.class)
    public void should_throw_exception_for_null_recordSet() {
        Rule rule = new UniqueValueRule(null, false);
        assertTrue(rule.run(null, messages));
    }

    @Test
    public void should_be_valid_for_empty_recordSet() {
        Rule rule = new UniqueValueRule("col1", false);

        assertTrue(rule.run(new RecordSet(entity(), false), messages));
    }

    @Test
    public void should_not_validate_if_empty_columns() {
        Rule rule = new UniqueValueRule(null, false);

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
        Rule rule = new UniqueValueRule("fake_column", false);

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
        Rule rule = new UniqueValueRule("col1", false, RuleLevel.ERROR);

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

    @Test
    public void should_be_valid_when_duplicate_values_in_different_expeditions() {
        Rule rule = new UniqueValueRule("col1", false, RuleLevel.ERROR);

        RecordSet recordSet = new RecordSet(entity(), false);

        List<Record> validRecords = getValidRecords();
        validRecords.forEach(recordSet::add);

        Record r1 = new GenericRecord();
        r1.set("urn:col1", "value3"); // this k:v is in the validRecords list
        r1.setExpeditionCode("exp2");
        r1.setProjectId(1);
        recordSet.add(r1);

        assertTrue(rule.run(recordSet, messages));
    }

    @Test
    public void should_not_be_valid_when_duplicate_values_in_different_expeditions_but_unique_across_project() {
        Rule rule = new UniqueValueRule("col1", true, RuleLevel.ERROR);

        RecordSet recordSet = new RecordSet(entity(), false);

        List<Record> validRecords = getValidRecords();
        validRecords.forEach(recordSet::add);

        Record r1 = new GenericRecord();
        r1.set("urn:col1", "value3"); // this k:v is in the validRecords list
        r1.setExpeditionCode("exp2");
        r1.setProjectId(1);
        recordSet.add(r1);

        assertFalse(rule.run(recordSet, messages));
        assertTrue(rule.hasError());

        EntityMessages expectedMessages = new EntityMessages("Samples");
        expectedMessages.addErrorMessage(
                "Unique value constraint did not pass",
                new Message("\"col1\" column is defined as unique across the entire project but some values used more than once: \"value3\"")
        );

        assertEquals(expectedMessages, messages);
    }

    private List<Record> getValidRecords() {
        List<Record> recordSet = new ArrayList<>();

        Record r1 = new GenericRecord();
        r1.set("urn:col1", "");
        r1.setExpeditionCode("exp");
        r1.setProjectId(1);
        recordSet.add(r1);
        Record r2 = new GenericRecord();
        r2.setExpeditionCode("exp");
        r2.setProjectId(1);
        r2.set("urn:col1", "value3");
        recordSet.add(r2);
        Record r3 = new GenericRecord();
        r3.setExpeditionCode("exp");
        r3.setProjectId(1);
        r3.set("urn:col1", "");
        recordSet.add(r3);
        return recordSet;
    }

    private List<Record> getInvalidRecords() {
        List<Record> recordSet = new ArrayList<>();

        for (String v : getInvalidRecordValues()) {
            Record r = new GenericRecord();
            r.set("urn:col1", v);
            r.setExpeditionCode("exp");
            r.setProjectId(1);
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