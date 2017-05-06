package biocode.fims.validation.rules;

import biocode.fims.models.records.GenericRecord;
import biocode.fims.models.records.Record;
import biocode.fims.models.records.RecordSet;
import biocode.fims.renderers.EntityMessages;
import biocode.fims.renderers.SimpleMessage;
import org.junit.Test;

import java.util.*;

import static org.junit.Assert.*;

/**
 * @author rjewing
 */
public class MinMaxNumberRuleTest extends AbstractRuleTest {

    @Test(expected = IllegalArgumentException.class)
    public void should_throw_exception_for_null_recordSet() {
        Rule rule = new MinMaxNumberRule(null, null);
        assertTrue(rule.run(null, messages));
    }

    @Test
    public void should_return_false_from_validConfiguration_if_only_1_column() {
        Rule rule = new MinMaxNumberRule("col1", null);

        List<String> messages = new ArrayList<>();

        assertFalse(rule.validConfiguration(messages, entity()));

        assertEquals(Collections.singletonList("Invalid MinMaxNumber Rule configuration. minimumColumn and " +
                "maximumColumn must not be null or empty"), messages);
    }

    @Test
    public void should_not_validate_if_only_1_column() {
        Rule rule = new MinMaxNumberRule("col1", null);

        assertFalse(rule.run(new RecordSet(entity()), messages));
        assertTrue(rule.hasError());

        EntityMessages expectedMessages = new EntityMessages("Samples");
        expectedMessages.addErrorMessage(
                "Invalid Rule Configuration. Contact Project Administrator.",
                new SimpleMessage("Invalid MinMaxNumber Rule configuration. minimumColumn and " +
                        "maximumColumn must not be null or empty")
        );

        assertEquals(expectedMessages, messages);
    }

    @Test
    public void should_be_valid_for_empty_recordSet() {
        Rule rule = new MinMaxNumberRule("col1", "col2");

        assertTrue(rule.run(new RecordSet(entity()), messages));
    }

    @Test
    public void should_not_be_valid_for_missing_value_from_min_col() {
        Rule rule = new MinMaxNumberRule("col1", "col2");

        RecordSet recordSet = new RecordSet(entity());

        Record r = new GenericRecord();
        r.set("urn:col2", "14");
        recordSet.add(r);

        assertFalse(rule.run(recordSet, messages));

        EntityMessages expectedMessages = new EntityMessages("Samples");
        expectedMessages.addWarningMessage(
                "Spreadsheet check",
                new SimpleMessage("Column \"col2\" exists but must have corresponding column \"col1\"")
        );

        assertEquals(expectedMessages, messages);
    }

    @Test
    public void should_not_be_valid_for_missing_value_from_max_col() {
        Rule rule = new MinMaxNumberRule("col1", "col2");

        RecordSet recordSet = new RecordSet(entity());

        Record r = new GenericRecord();
        r.set("urn:col1", "14");
        recordSet.add(r);

        assertFalse(rule.run(recordSet, messages));

        EntityMessages expectedMessages = new EntityMessages("Samples");
        expectedMessages.addWarningMessage(
                "Spreadsheet check",
                new SimpleMessage("Column \"col1\" exists but must have corresponding column \"col2\"")
        );

        assertEquals(expectedMessages, messages);
    }

    @Test
    public void should_not_be_valid_for_non_numeric_values() {
        Rule rule = new MinMaxNumberRule("col1", "col2");

        RecordSet recordSet = new RecordSet(entity());

        Record r = new GenericRecord();
        r.set("urn:col1", "14");
        r.set("urn:col2", "14.00001");
        recordSet.add(r);

        Record r2 = new GenericRecord();
        r2.set("urn:col1", "a123b");
        r2.set("urn:col2", "(10)");
        recordSet.add(r2);

        assertFalse(rule.run(recordSet, messages));

        EntityMessages expectedMessages = new EntityMessages("Samples");
        expectedMessages.addWarningMessage(
                "Number outside of range",
                new SimpleMessage("non-numeric value \"a123b\" for column \"col1\"")
        );
        expectedMessages.addWarningMessage(
                "Number outside of range",
                new SimpleMessage("non-numeric value \"(10)\" for column \"col2\"")
        );

        assertEquals(expectedMessages, messages);
    }

    @Test
    public void should_not_be_valid_if_2nd_col_is_greater_then_first() {
        Rule rule = new MinMaxNumberRule("col1", "col2");

        RecordSet recordSet = new RecordSet(entity());

        Record r = new GenericRecord();
        r.set("urn:col1", "14");
        r.set("urn:col2", "14.00001");
        recordSet.add(r);

        Record r2 = new GenericRecord();
        r2.set("urn:col1", "122");
        r2.set("urn:col2", ".12");
        recordSet.add(r2);

        assertFalse(rule.run(recordSet, messages));

        EntityMessages expectedMessages = new EntityMessages("Samples");
        expectedMessages.addWarningMessage(
                "Number outside of range",
                new SimpleMessage("Illegal values! col1 = 122 while col2 = .12")
        );

        assertEquals(expectedMessages, messages);
    }

    @Test
    public void should_be_valid() {
        Rule rule = new MinMaxNumberRule("col1", "col2");

        RecordSet recordSet = new RecordSet(entity());

        Record r = new GenericRecord();
        r.set("urn:col1", "14");
        r.set("urn:col2", "14.00001");
        recordSet.add(r);

        Record r2 = new GenericRecord();
        r2.set("urn:col1", "");
        r2.set("urn:col2", "");
        recordSet.add(r2);

        assertTrue(rule.run(recordSet, messages));
    }

}