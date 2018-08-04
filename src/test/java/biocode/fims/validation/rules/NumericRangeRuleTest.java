package biocode.fims.validation.rules;

import biocode.fims.models.records.GenericRecord;
import biocode.fims.models.records.Record;
import biocode.fims.models.records.RecordSet;
import biocode.fims.validation.messages.EntityMessages;
import biocode.fims.validation.messages.Message;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * @author rjewing
 */
public class NumericRangeRuleTest extends AbstractRuleTest {

    @Test(expected = IllegalArgumentException.class)
    public void should_throw_exception_for_null_recordSet() {
        Rule rule = new NumericRangeRule(null, "");
        assertTrue(rule.run(null, messages));
    }

    @Test
    public void should_be_valid_for_empty_recordSet() {
        Rule rule = new NumericRangeRule("col1", ">0");

        assertTrue(rule.run(new RecordSet(entity(), false), messages));
    }

    @Test
    public void should_not_validate_if_empty_columns() {
        Rule rule = new NumericRangeRule(null, ">0");

        assertFalse(rule.run(new RecordSet(entity(), false), messages));
        assertTrue(rule.hasError());

        EntityMessages expectedMessages = new EntityMessages("Samples");
        expectedMessages.addErrorMessage(
                "Invalid Rule Configuration. Contact Project Administrator.",
                new Message("Invalid NumericRange Rule configuration. Column must not be blank or null.")
        );

        assertEquals(expectedMessages, messages);
    }

    @Test
    public void should_not_validate_if_no_range() {
        Rule rule = new NumericRangeRule("col1", null);

        assertFalse(rule.run(new RecordSet(entity(), false), messages));
        assertTrue(rule.hasError());

        EntityMessages expectedMessages = new EntityMessages("Samples");
        expectedMessages.addErrorMessage(
                "Invalid Rule Configuration. Contact Project Administrator.",
                new Message("Invalid NumericRange Rule configuration. range must not be blank or null.")
        );

        assertEquals(expectedMessages, messages);
    }

    @Test
    public void should_not_validate_if_malformed_range() {
        Rule rule = new NumericRangeRule("col1", "0");
        assertFalse(rule.run(new RecordSet(entity(), false), messages));

        EntityMessages expectedMessages = new EntityMessages("Samples");
        expectedMessages.addErrorMessage(
                "Invalid Rule Configuration. Contact Project Administrator.",
                new Message("Invalid NumericRange Rule configuration. Could not parse range \"0\"")
        );

        assertEquals(expectedMessages, messages);


        messages = new EntityMessages("Samples");
        rule = new NumericRangeRule("col1", "abc");
        assertFalse(rule.run(new RecordSet(entity(), false), messages));

        expectedMessages = new EntityMessages("Samples");
        expectedMessages.addErrorMessage(
                "Invalid Rule Configuration. Contact Project Administrator.",
                new Message("Invalid NumericRange Rule configuration. Could not parse range \"abc\"")
        );

        assertEquals(expectedMessages, messages);


        messages = new EntityMessages("Samples");
        rule = new NumericRangeRule("col1", "=123");
        assertFalse(rule.run(new RecordSet(entity(), false), messages));

        expectedMessages = new EntityMessages("Samples");
        expectedMessages.addErrorMessage(
                "Invalid Rule Configuration. Contact Project Administrator.",
                new Message("Invalid NumericRange Rule configuration. Could not parse range \"=123\"")
        );

        assertEquals(expectedMessages, messages);
    }

    @Test
    public void should_be_valid_when_uknown_allowed() {

        Rule rule = new NumericRangeRule("col1", "<10");

        RecordSet recordSet = new RecordSet(entity(), false);
        recordSet.entity().getAttribute("col1").setAllowUnknown(true);

        Record r1 = new GenericRecord();
        r1.set("urn:col1", "-11");
        Record r2 = new GenericRecord();
        r2.set("urn:col1", "11");
        Record r3 = new GenericRecord();
        r3.set("urn:col1", "unKnoWn");
        recordSet.add(r1);
        recordSet.add(r2);
        recordSet.add(r3);

        assertFalse(rule.run(recordSet, messages));

        EntityMessages expectedMessages = new EntityMessages("Samples");
        expectedMessages.addWarningMessage(
                "Invalid number format",
                new Message("Value \"11\" out of range for \"col1\" using range validation = \"<10\""
                )
        );

        assertEquals(expectedMessages, messages);
    }

    @Test
    public void should_not_be_valid_when_not_unknown_allowed() {

        Rule rule = new NumericRangeRule("col1", "<10");

        RecordSet recordSet = new RecordSet(entity(), false);

        Record r1 = new GenericRecord();
        r1.set("urn:col1", "unknown");
        recordSet.add(r1);

        assertFalse(rule.run(recordSet, messages));

        EntityMessages expectedMessages = new EntityMessages("Samples");
        expectedMessages.addWarningMessage(
                "Invalid number format",
                new Message("Value \"unknown\" out of range for \"col1\" using range validation = \"<10\""
                )
        );

        assertEquals(expectedMessages, messages);
    }

    @Test
    public void should_not_be_valid_when_outside_less_then_range() {

        Rule rule = new NumericRangeRule("col1", "<10");

        RecordSet recordSet = new RecordSet(entity(), false);

        Record r1 = new GenericRecord();
        r1.set("urn:col1", "-11");
        Record r2 = new GenericRecord();
        r2.set("urn:col1", "11");
        recordSet.add(r1);
        recordSet.add(r2);

        assertFalse(rule.run(recordSet, messages));

        EntityMessages expectedMessages = new EntityMessages("Samples");
        expectedMessages.addWarningMessage(
                "Invalid number format",
                new Message("Value \"11\" out of range for \"col1\" using range validation = \"<10\""
                )
        );

        assertEquals(expectedMessages, messages);
    }

    @Test
    public void should_not_be_valid_when_outside_less_then_equals_range() {

        Rule rule = new NumericRangeRule("col1", "<=10");

        RecordSet recordSet = new RecordSet(entity(), false);

        Record r1 = new GenericRecord();
        r1.set("urn:col1", "10.00");
        Record r2 = new GenericRecord();
        r2.set("urn:col1", "11");
        recordSet.add(r1);
        recordSet.add(r2);

        assertFalse(rule.run(recordSet, messages));

        EntityMessages expectedMessages = new EntityMessages("Samples");
        expectedMessages.addWarningMessage(
                "Invalid number format",
                new Message("Value \"11\" out of range for \"col1\" using range validation = \"<=10\""
                )
        );

        assertEquals(expectedMessages, messages);
    }

    @Test
    public void should_not_be_valid_when_outside_greater_then_range() {

        Rule rule = new NumericRangeRule("col1", ">10");

        RecordSet recordSet = new RecordSet(entity(), false);

        Record r1 = new GenericRecord();
        r1.set("urn:col1", "0.00");
        Record r2 = new GenericRecord();
        r2.set("urn:col1", "11000000");
        recordSet.add(r1);
        recordSet.add(r2);

        assertFalse(rule.run(recordSet, messages));

        EntityMessages expectedMessages = new EntityMessages("Samples");
        expectedMessages.addWarningMessage(
                "Invalid number format",
                new Message("Value \"0.00\" out of range for \"col1\" using range validation = \">10\""
                )
        );

        assertEquals(expectedMessages, messages);
    }

    @Test
    public void should_not_be_valid_when_outside_greater_then_equals_range() {

        Rule rule = new NumericRangeRule("col1", ">=0");

        RecordSet recordSet = new RecordSet(entity(), false);

        Record r1 = new GenericRecord();
        r1.set("urn:col1", "0.00");
        Record r2 = new GenericRecord();
        r2.set("urn:col1", "-0.00001");
        recordSet.add(r1);
        recordSet.add(r2);

        assertFalse(rule.run(recordSet, messages));

        EntityMessages expectedMessages = new EntityMessages("Samples");
        expectedMessages.addWarningMessage(
                "Invalid number format",
                new Message("Value \"-0.00001\" out of range for \"col1\" using range validation = \">=0\""
                )
        );

        assertEquals(expectedMessages, messages);
    }

    @Test
    public void should_not_be_valid_when_outside_multiple_range() {

        Rule rule = new NumericRangeRule("col1", ">=0 | <=10");

        RecordSet recordSet = new RecordSet(entity(), false);

        Record r1 = new GenericRecord();
        r1.set("urn:col1", "0.00");
        Record r2 = new GenericRecord();
        r2.set("urn:col1", "-0.00001");
        Record r3 = new GenericRecord();
        r3.set("urn:col1", "11");
        recordSet.add(r1);
        recordSet.add(r2);
        recordSet.add(r3);

        assertFalse(rule.run(recordSet, messages));

        EntityMessages expectedMessages = new EntityMessages("Samples");
        expectedMessages.addWarningMessage(
                "Invalid number format",
                new Message("Value \"-0.00001\" out of range for \"col1\" using range validation = \">=0 | <=10\""
                )
        );
        expectedMessages.addWarningMessage(
                "Invalid number format",
                new Message("Value \"11\" out of range for \"col1\" using range validation = \">=0 | <=10\""
                )
        );

        assertEquals(expectedMessages, messages);
    }
}