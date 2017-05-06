package biocode.fims.validation.rules;

import biocode.fims.models.records.GenericRecord;
import biocode.fims.models.records.Record;
import biocode.fims.models.records.RecordSet;
import biocode.fims.renderers.EntityMessages;
import biocode.fims.renderers.SimpleMessage;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import static org.junit.Assert.*;

/**
 * @author rjewing
 */
public class CompositeUniqueValueRuleTest extends AbstractRuleTest {

    private CompositeUniqueValueRule rule;

    @Before
    public void setUp() {
        this.rule = new CompositeUniqueValueRule();
    }

    @Test(expected = IllegalArgumentException.class)
    public void should_throw_exception_for_null_recordSet() {
        assertTrue(rule.run(null, messages));
    }

    @Test(expected = UnsupportedOperationException.class)
    public void should_throw_exception_for_setColumn() {
        rule.setColumn("test");
    }

    @Test
    public void should_be_valid_for_empty_recordSet() {
        assertTrue(rule.run(new RecordSet(entity()), messages));
    }

    @Test
    public void should_not_be_valid_when_duplicate_values() {
        RecordSet recordSet = new RecordSet(entity());

        List<Record> invalidRecords = getInvalidRecords();
        invalidRecords.forEach(recordSet::add);

        List<Record> validRecords = getValidRecords();
        validRecords.forEach(recordSet::add);

        rule.setColumns(new LinkedList<>(Arrays.asList("col1", "col2")));
        rule.setLevel(RuleLevel.ERROR);

        assertFalse(rule.run(recordSet, messages));
        assertEquals(RuleLevel.ERROR, rule.level());

        EntityMessages expectedMessages = new EntityMessages("Samples");
        expectedMessages.addErrorMessage(
                "Unique value constraint did not pass",
                new SimpleMessage(
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