package biocode.fims.validation.rules;

import biocode.fims.models.records.GenericRecord;
import biocode.fims.models.records.Record;
import biocode.fims.models.records.RecordSet;
import biocode.fims.renderers.EntityMessages;
import biocode.fims.renderers.SimpleMessage;
import org.junit.Before;
import org.junit.Test;

import java.util.*;

import static org.junit.Assert.*;

/**
 * @author rjewing
 */
public class MinMaxNumberRuleTest extends AbstractRuleTest {

    private MinMaxNumberRule rule;

    @Before
    public void setUp() {
        this.rule = new MinMaxNumberRule();
    }

    @Test(expected = IllegalArgumentException.class)
    public void should_throw_exception_for_null_recordSet() {
        assertTrue(rule.run(null, messages));
    }

    @Test
    public void should_return_false_from_validConfiguration_if_only_1_column() {
        rule.setColumns(new LinkedList<>(Arrays.asList("col1")));

        List<String> messages = new ArrayList<>();

        assertFalse(rule.validConfiguration(messages));

        assertEquals(Collections.singletonList("Invalid MinMaxNumber Rule configuration. 2 columns should be specified, " +
                "but we found 1. Talk to your project administrator to fix the issue"), messages);
    }

    @Test
    public void should_return_false_from_validConfiguration_if_more_then_2_columns() {
        rule.setColumns(new LinkedList<>(Arrays.asList("col1", "col2", "col3")));
        List<String> messages = new ArrayList<>();

        assertFalse(rule.validConfiguration(messages));

        assertEquals(Collections.singletonList("Invalid MinMaxNumber Rule configuration. 2 columns should be specified, " +
                "but we found 3. Talk to your project administrator to fix the issue"), messages);
    }

    @Test
    public void should_not_validate_if_only_1_column() {
        rule.setColumns(new LinkedList<>(Arrays.asList("col1")));
        assertFalse(rule.run(new RecordSet(entity()), messages));
        assertTrue(rule.hasError());

        EntityMessages expectedMessages = new EntityMessages("Samples");
        expectedMessages.addErrorMessage(
                "Invalid Rule Configuration",
                new SimpleMessage("Invalid MinMaxNumber Rule configuration. 2 columns should be specified, " +
                        "but we found 1. Talk to your project administrator to fix the issue")
        );

        assertEquals(expectedMessages, messages);
    }

    @Test
    public void should_not_validate_if_more_then_2_columns() {
        rule.setColumns(new LinkedList<>(Arrays.asList("col1", "col2", "col3")));
        assertFalse(rule.run(new RecordSet(entity()), messages));
        assertTrue(rule.hasError());

        EntityMessages expectedMessages = new EntityMessages("Samples");
        expectedMessages.addErrorMessage(
                "Invalid Rule Configuration",
                new SimpleMessage("Invalid MinMaxNumber Rule configuration. 2 columns should be specified, " +
                        "but we found 3. Talk to your project administrator to fix the issue")
        );

        assertEquals(expectedMessages, messages);
    }

    @Test
    public void should_be_valid_for_empty_recordSet() {
        rule.setColumns(new LinkedList<>(Arrays.asList("col1", "col2")));

        assertTrue(rule.run(new RecordSet(entity()), messages));
    }

    @Test
    public void should_not_be_valid_for_missing_value_from_min_col() {
        rule.setColumns(new LinkedList<>(Arrays.asList("col1", "col2")));

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
        rule.setColumns(new LinkedList<>(Arrays.asList("col1", "col2")));

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
        rule.setColumns(new LinkedList<>(Arrays.asList("col1", "col2")));

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
        rule.setColumns(new LinkedList<>(Arrays.asList("col1", "col2")));

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
        rule.setColumns(new LinkedList<>(Arrays.asList("col1", "col2")));

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