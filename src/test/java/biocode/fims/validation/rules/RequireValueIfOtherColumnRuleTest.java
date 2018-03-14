package biocode.fims.validation.rules;

import biocode.fims.models.records.GenericRecord;
import biocode.fims.models.records.Record;
import biocode.fims.models.records.RecordSet;
import biocode.fims.validation.messages.EntityMessages;
import biocode.fims.validation.messages.Message;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

/**
 * @author rjewing
 */
public class RequireValueIfOtherColumnRuleTest extends AbstractRuleTest {

    @Test(expected = IllegalArgumentException.class)
    public void should_throw_exception_for_null_recordSet() {
        Rule rule = new RequireValueIfOtherColumnRule(null, null);
        assertTrue(rule.run(null, messages));
    }

    @Test
    public void should_be_valid_for_empty_recordSet() {
        Rule rule = new RequireValueIfOtherColumnRule("col1", "col2");

        assertTrue(rule.run(new RecordSet(entity(), false), messages));
    }

    @Test
    public void should_not_validate_if_empty_column() {
        Rule rule = new RequireValueIfOtherColumnRule(null, "col2");

        assertFalse(rule.run(new RecordSet(entity(), false), messages));
        assertTrue(rule.hasError());

        EntityMessages expectedMessages = new EntityMessages("Samples");
        expectedMessages.addErrorMessage(
                "Invalid Rule Configuration. Contact Project Administrator.",
                new Message("Invalid RequireValueIfOtherColumn Rule configuration. Column must not be blank or null.")
        );

        assertEquals(expectedMessages, messages);
    }

    @Test
    public void should_not_validate_if_empty_otherColumn() {
        Rule rule = new RequireValueIfOtherColumnRule("col1", "");

        assertFalse(rule.run(new RecordSet(entity(), false), messages));
        assertTrue(rule.hasError());

        EntityMessages expectedMessages = new EntityMessages("Samples");
        expectedMessages.addErrorMessage(
                "Invalid Rule Configuration. Contact Project Administrator.",
                new Message("Invalid RequireValueIfOtherColumn Rule configuration. otherColumn must not be blank or null.")
        );

        assertEquals(expectedMessages, messages);
    }

    @Test
    public void should_not_validate_if_no_attribute_for_otherColumn() {
        Rule rule = new RequireValueIfOtherColumnRule("col2", "fake_column");

        assertFalse(rule.run(new RecordSet(entity(), false), messages));
        assertTrue(rule.hasError());

        EntityMessages expectedMessages = new EntityMessages("Samples");
        expectedMessages.addErrorMessage(
                "Invalid Rule Configuration. Contact Project Administrator.",
                new Message("Invalid RequireValueIfOtherColumn Rule configuration. Could not find Attribute for column: fake_column in entity: Samples")
        );

        assertEquals(expectedMessages, messages);
    }

    @Test
    public void should_not_be_valid_when_column_missing_value() {
        Rule rule = new RequireValueIfOtherColumnRule("col1", "col2");

        RecordSet recordSet = new RecordSet(entity(), false);

        List<Record> validRecords = getValidRecords();
        validRecords.forEach(recordSet::add);

        Record r1 = new GenericRecord();
        r1.set("urn:col1", "value");
        r1.set("urn:col2", "");
        recordSet.add(r1);

        assertFalse(rule.run(recordSet, messages));
        assertFalse(rule.hasError());

        EntityMessages expectedMessages = new EntityMessages("Samples");
        expectedMessages.addWarningMessage(
                "Dependent column value check",
                new Message(
                        "\"col2\" has value \"value\", but associated column \"col1\" has no value"
                )
        );

        assertEquals(expectedMessages, messages);
    }

    private List<Record> getValidRecords() {
        List<Record> recordSet = new ArrayList<>();

        Record r1 = new GenericRecord();
        r1.set("urn:col1", "");
        r1.set("urn:col2", "");
        recordSet.add(r1);
        Record r2 = new GenericRecord();
        r2.set("urn:col1", "value3");
        r2.set("urn:col2", "value1");
        recordSet.add(r2);
        Record r3 = new GenericRecord();
        r3.set("urn:col1", "");
        r3.set("urn:col2", "value");
        recordSet.add(r3);

        return recordSet;
    }

}