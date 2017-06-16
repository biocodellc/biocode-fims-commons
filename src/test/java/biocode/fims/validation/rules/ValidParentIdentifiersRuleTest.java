package biocode.fims.validation.rules;

import biocode.fims.digester.Attribute;
import biocode.fims.digester.Entity;
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
public class ValidParentIdentifiersRuleTest extends AbstractRuleTest {

    @Test(expected = IllegalArgumentException.class)
    public void should_throw_exception_for_null_recordSet() {
        Rule rule = new ValidParentIdentifiersRule();
        assertTrue(rule.run(null, messages));
    }

    @Test
    public void should_be_valid_for_empty_recordSet() {
        Rule rule = new ValidParentIdentifiersRule();

        RecordSet recordSet = new RecordSet(entity());
        RecordSet parentSet = new RecordSet(parentEntity());
        recordSet.setParent(parentSet);

        assertTrue(rule.run(recordSet, messages));
    }

    @Test
    public void should_be_valid_for_non_child_recordSet() {
        Rule rule = new ValidParentIdentifiersRule();

        RecordSet recordSet = new RecordSet(parentEntity());

        assertTrue(rule.run(recordSet, messages));
    }

    @Test(expected = IllegalStateException.class)
    public void should_throw_exception_if_parent_entity_is_null() {
        Rule rule = new ValidParentIdentifiersRule();

        rule.run(new RecordSet(entity()), messages);
    }

    @Test
    public void should_not_validate_if_parent_identifiers_dont_exist() {
        Rule rule = new ValidParentIdentifiersRule();

        RecordSet parentSet = new RecordSet(parentEntity());
        RecordSet recordSet = new RecordSet(entity());
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