package biocode.fims.validation.rules;

import biocode.fims.digester.Attribute;
import biocode.fims.digester.Entity;
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
public class RequiredValueRuleTest extends AbstractRuleTest {

    @Test(expected = IllegalArgumentException.class)
    public void should_throw_exception_for_null_recordSet() {
        Rule rule = new RequiredValueRule(null);

        assertTrue(rule.run(null, messages));
    }

    @Test
    public void should_be_valid_for_empty_recordSet() {
        Rule rule = new RequiredValueRule(new LinkedHashSet<>(Arrays.asList("col1", "col2")));
        assertTrue(rule.run(new RecordSet(entity()), messages));
    }

    @Test
    public void should_not_validate_if_empty_columns() {
        Rule rule = new RequiredValueRule(new LinkedHashSet<>(Collections.emptyList()));

        assertFalse(rule.run(new RecordSet(entity()), messages));
        assertTrue(rule.hasError());

        EntityMessages expectedMessages = new EntityMessages("Samples");
        expectedMessages.addErrorMessage(
                "Invalid Rule Configuration. Contact Project Administrator.",
                new Message("Invalid RequiredValue Rule configuration. columns must not be empty.")
        );

        assertEquals(expectedMessages, messages);
    }

    @Test
    public void should_not_be_valid_when_column_missing_value() {
        Rule rule = new RequiredValueRule(new LinkedHashSet<>(Arrays.asList("col1", "col2", "col3")), RuleLevel.ERROR);

        RecordSet recordSet = new RecordSet(entity());

        Record r1 = new GenericRecord();
        r1.set("urn:col1", "");
        r1.set("urn:col2", "");
        r1.set("urn:col3", "value");
        recordSet.add(r1);

        Record r2 = new GenericRecord();
        r2.set("urn:col1", "");
        r2.set("urn:col2", "some value");
        r2.set("urn:col3", "value");
        recordSet.add(r2);

        assertFalse(rule.run(recordSet, messages));
        assertTrue(rule.hasError());

        EntityMessages expectedMessages = new EntityMessages("Samples");
        expectedMessages.addErrorMessage(
                "Missing column(s)",
                new Message("\"col1\" has a missing cell value")
        );
        expectedMessages.addErrorMessage(
                "Missing column(s)",
                new Message("\"col2\" has a missing cell value")
        );

        assertEquals(expectedMessages, messages);
    }

    @Override
    protected Entity entity() {
        Entity e = super.entity();
        e.addAttribute(new Attribute("col3", "urn:col3"));
        return e;
    }
}