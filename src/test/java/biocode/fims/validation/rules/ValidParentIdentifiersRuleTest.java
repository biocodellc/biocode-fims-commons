package biocode.fims.validation.rules;

import biocode.fims.projectConfig.models.Attribute;
import biocode.fims.projectConfig.models.Entity;
import biocode.fims.records.GenericRecord;
import biocode.fims.records.Record;
import biocode.fims.records.RecordSet;
import biocode.fims.validation.messages.EntityMessages;
import biocode.fims.validation.messages.Message;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * @author rjewing
 */
public class ValidParentIdentifiersRuleTest extends AbstractRuleTest {

    @Test(expected = IllegalArgumentException.class)
    public void should_throw_exception_for_null_recordSet() {
        Rule rule = new ValidParentIdentifiersRule();
        assertTrue(rule.run(null, messages));
    }

    @Test
    public void should_be_valid_for_empty_recordSet() {
        Rule rule = new ValidParentIdentifiersRule();

        RecordSet recordSet = new RecordSet(entity(), false);
        RecordSet parentSet = new RecordSet(parentEntity(), false);
        recordSet.setParent(parentSet);

        assertTrue(rule.run(recordSet, messages));
    }

    @Test
    public void should_be_valid_for_non_child_recordSet() {
        Rule rule = new ValidParentIdentifiersRule();

        RecordSet recordSet = new RecordSet(parentEntity(), false);

        assertTrue(rule.run(recordSet, messages));
    }

    @Test(expected = IllegalStateException.class)
    public void should_throw_exception_if_parent_entity_is_null() {
        Rule rule = new ValidParentIdentifiersRule();

        rule.run(new RecordSet(entity(), false), messages);
    }

    @Test
    public void should_not_validate_if_parent_identifiers_dont_exist() {
        Rule rule = new ValidParentIdentifiersRule();

        RecordSet parentSet = new RecordSet(parentEntity(), false);
        RecordSet recordSet = new RecordSet(entity(), false);
        recordSet.setParent(parentSet);

        Record parent = new GenericRecord();
        parent.set("eventId", "event1");
        parentSet.add(parent);

        Record c1 = new GenericRecord();
        c1.set("eventId", "event2");
        recordSet.add(c1);

        Record c2 = new GenericRecord();
        recordSet.add(c2);

        Record c3 = new GenericRecord();
        c3.set("eventId", "event1");
        recordSet.add(c3);

        assertFalse(rule.run(recordSet, messages));
        assertTrue(rule.hasError());

        EntityMessages expectedMessages = new EntityMessages("Samples");
        expectedMessages.addErrorMessage(
                "Invalid parent identifier(s)",
                new Message("The following identifiers do not exist in the parent entity \"event\": [\"event2\", \"\"]")
        );

        assertEquals(expectedMessages, messages);
    }

    @Test
    public void should_not_validate_if_parent_identifiers_dont_exist_in_expedition() {
        Rule rule = new ValidParentIdentifiersRule();

        RecordSet parentSet = new RecordSet(parentEntity(), false);
        RecordSet recordSet = new RecordSet(entity(), false);
        recordSet.setParent(parentSet);

        Record parent = new GenericRecord();
        parent.set("eventId", "event1");
        parent.setProjectId(1);
        parent.setExpeditionCode("exp2");
        parentSet.add(parent);

        Record c1 = new GenericRecord();
        c1.set("eventId", "event1");
        c1.setProjectId(1);
        c1.setExpeditionCode("exp1");
        recordSet.add(c1);

        assertFalse(rule.run(recordSet, messages));
        assertTrue(rule.hasError());

        EntityMessages expectedMessages = new EntityMessages("Samples");
        expectedMessages.addErrorMessage(
                "Invalid parent identifier(s)",
                new Message("The following identifiers do not exist in the parent entity \"event\": [\"event1\"]")
        );

        assertEquals(expectedMessages, messages);
    }

    @Override
    protected Entity entity() {
        Entity e = super.entity();
        e.setParentEntity("event");

        e.addAttribute(new Attribute("eventId", "eventId"));
        return e;
    }

    private Entity parentEntity() {
        Entity e = new Entity("event", "someURI");
        e.setUniqueKey("eventId");

        e.addAttribute(new Attribute("eventId", "eventId"));
        return e;
    }
}