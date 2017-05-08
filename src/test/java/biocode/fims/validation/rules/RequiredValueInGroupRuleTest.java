package biocode.fims.validation.rules;

import biocode.fims.digester.Attribute;
import biocode.fims.digester.Entity;
import biocode.fims.models.records.GenericRecord;
import biocode.fims.models.records.Record;
import biocode.fims.models.records.RecordSet;
import biocode.fims.renderers.EntityMessages;
import biocode.fims.renderers.SimpleMessage;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;

import static org.junit.Assert.*;

/**
 * @author rjewing
 */
public class RequiredValueInGroupRuleTest extends AbstractRuleTest {

    @Test(expected = IllegalArgumentException.class)
    public void should_throw_exception_for_null_recordSet() {
        Rule rule = new RequiredValueInGroupRule(null);

        assertTrue(rule.run(null, messages));
    }

    @Test
    public void should_be_valid_for_empty_recordSet() {
        Rule rule = new RequiredValueInGroupRule(Arrays.asList("col2", "col3"));
        assertTrue(rule.run(new RecordSet(entity()), messages));
    }

    @Test
    public void should_not_validate_if_empty_columns() {
        Rule rule = new RequiredValueInGroupRule(Collections.emptyList());

        assertFalse(rule.run(new RecordSet(entity()), messages));
        assertTrue(rule.hasError());

        EntityMessages expectedMessages = new EntityMessages("Samples");
        expectedMessages.addErrorMessage(
                "Invalid Rule Configuration. Contact Project Administrator.",
                new SimpleMessage("Invalid RequiredValueInGroup Rule configuration. columns must not be empty.")
        );

        assertEquals(expectedMessages, messages);
    }

    @Test
    public void should_not_be_valid_when_all_columns_are_missing_value() {
        Rule rule = new RequiredValueInGroupRule(Arrays.asList("col2", "col3"), RuleLevel.ERROR);

        RecordSet recordSet = new RecordSet(entity());

        Record r1 = new GenericRecord();
        r1.set("urn:col1", "record1");
        r1.set("urn:col2", "");
        r1.set("urn:col3", "value");
        recordSet.add(r1);

        Record r2 = new GenericRecord();
        r2.set("urn:col1", "record2");
        r2.set("urn:col2", "some value");
        r2.set("urn:col3", "value");
        recordSet.add(r2);

        Record r3 = new GenericRecord();
        r3.set("urn:col1", "record3");
        r3.set("urn:col2", "some value");
        r3.set("urn:col3", "");
        recordSet.add(r3);

        Record r4 = new GenericRecord();
        r4.set("urn:col1", "record4");
        r4.set("urn:col2", "");
        r4.set("urn:col3", "");
        recordSet.add(r4);

        assertFalse(rule.run(recordSet, messages));
        assertTrue(rule.hasError());

        EntityMessages expectedMessages = new EntityMessages("Samples");
        expectedMessages.addErrorMessage(
                "Missing column from group",
                new SimpleMessage("row with col1=record4 must have a value in at least 1 of the columns: " +
                        "[\"col2\",\"col3\"]")
        );

        assertEquals(expectedMessages, messages);
    }

    @Override
    protected Entity entity() {
        Entity e = super.entity();
        e.setUniqueKey("col1");
        e.addAttribute(new Attribute("col3", "urn:col3"));
        return e;
    }
}