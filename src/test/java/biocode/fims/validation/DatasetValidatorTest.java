package biocode.fims.validation;

import biocode.fims.digester.Attribute;
import biocode.fims.digester.Entity;
import biocode.fims.digester.Mapping;
import biocode.fims.models.records.GenericRecord;
import biocode.fims.models.records.Record;
import biocode.fims.models.records.RecordSet;
import biocode.fims.projectConfig.ProjectConfig;
import biocode.fims.renderers.EntityMessages;
import biocode.fims.renderers.SimpleMessage;
import biocode.fims.validation.rules.RequiredValueRule;
import org.junit.Test;

import java.util.*;

import static org.junit.Assert.*;

/**
 * @author rjewing
 */
public class DatasetValidatorTest {

    @Test
    public void should_validate_all_recordSets() {
        RecordValidatorFactory validatorFactory = new RecordValidatorFactory(new HashMap<>());
        List<RecordSet> recordSets = new LinkedList<>();

        RecordSet events = new RecordSet(entity1());
        recordSets.add(events);
        RecordSet samples = new RecordSet(entity2());
        samples.setParent(events);
        recordSets.add(samples);

        Record e1 = new GenericRecord();
        e1.set("eventId", "event1");
        events.add(e1);

        Record e2 = new GenericRecord();
        e2.set("eventId", "event2");
        e2.set("col1", "someValue");
        events.add(e2);

        Record s1 = new GenericRecord();
        s1.set("sampleId", "1");
        s1.set("eventId", "event1");
        samples.add(s1);

        Record s2 = new GenericRecord();
        s2.set("sampleId", "2");
        s2.set("eventId", "event1");
        samples.add(s2);

        DatasetValidator validator = new DatasetValidator(validatorFactory, recordSets, config());

        assertFalse(validator.validate());
        assertFalse(validator.hasError());

        List<EntityMessages> messages = validator.messages();
        assertEquals(1, messages.size());

        EntityMessages expectedMessages = new EntityMessages("event", "sheet1");
        expectedMessages.addWarningMessage(
                "Missing column(s)",
                new SimpleMessage("\"col1\" has a missing cell value")
        );
        assertEquals(expectedMessages, messages.get(0));
    }

    @Test
    public void should_validate_if_parent_is_on_multi_entity_sheet_and_recordSet_has_records_with_same_identifier_and_same_values() {
        RecordValidatorFactory validatorFactory = new RecordValidatorFactory(new HashMap<>());
        List<RecordSet> recordSets = new LinkedList<>();

        RecordSet events = new RecordSet(entity1());
        RecordSet samples = new RecordSet(entity2());
        samples.setParent(events);

        recordSets.add(events);
        recordSets.add(samples);

        Record e1 = new GenericRecord();
        e1.set("eventId", "event1");
        e1.set("col1", "someValue");
        events.add(e1);

        Record e2 = new GenericRecord();
        e2.set("eventId", "event1");
        e2.set("col1", "someValue");
        events.add(e2);

        Record s1 = new GenericRecord();
        s1.set("sampleId", "1");
        s1.set("eventId", "event1");
        samples.add(s1);


        DatasetValidator validator = new DatasetValidator(validatorFactory, recordSets, config());

        assertTrue(validator.validate());
        assertFalse(validator.hasError());
    }

    @Test
    public void should_not_validate_if_parent_is_on_multi_entity_sheet_and_recordSet_has_records_with_same_identifier_and_different_values() {
        RecordValidatorFactory validatorFactory = new RecordValidatorFactory(new HashMap<>());
        List<RecordSet> recordSets = new LinkedList<>();

        RecordSet events = new RecordSet(entity1());
        RecordSet samples = new RecordSet(entity2());
        samples.setParent(events);

        recordSets.add(events);
        recordSets.add(samples);

        Record e1 = new GenericRecord();
        e1.set("eventId", "event1");
        e1.set("col1", "differentValue");
        events.add(e1);

        Record e2 = new GenericRecord();
        e2.set("eventId", "event1");
        e2.set("col1", "someValue");
        events.add(e2);

        Record s1 = new GenericRecord();
        s1.set("sampleId", "1");
        s1.set("eventId", "event1");
        samples.add(s1);


        DatasetValidator validator = new DatasetValidator(validatorFactory, recordSets, config());

        assertFalse(validator.validate());
        assertTrue(validator.hasError());

        List<EntityMessages> messages = validator.messages();
        assertEquals(1, messages.size());

        EntityMessages expectedMessages = new EntityMessages("event", "sheet1");
        expectedMessages.addErrorMessage(
                "Unique value constraint did not pass",
                new SimpleMessage("\"eventId\" column is defined as unique but some values used more than once: \"event1\"")
        );
        expectedMessages.addErrorMessage(
                "Duplicate parent records",
                new SimpleMessage("Duplicate \"eventId\" values, however the other columns are not the same.")
        );
        assertEquals(expectedMessages, messages.get(0));

    }

    private ProjectConfig config() {
        Mapping mapping = new Mapping();

        mapping.addEntity(entity1());
        mapping.addEntity(entity2());

        return new ProjectConfig(mapping, null, null);
    }

    private Entity entity1() {
        Entity e = new Entity("event");
        e.setUniqueKey("eventId");
        e.setWorksheet("sheet1");

        e.addAttribute(new Attribute("eventId", "eventId"));
        e.addAttribute(new Attribute("col1", "col1"));

        e.addRule(new RequiredValueRule(new LinkedHashSet<>(Collections.singletonList("col1"))));

        return e;
    }

    private Entity entity2() {
        Entity e = new Entity("sample");
        e.setParentEntity("event");
        e.setUniqueKey("sampleId");
        e.setWorksheet("sheet1");

        e.addAttribute(new Attribute("sampleId", "sampleId"));
        e.addAttribute(new Attribute("eventId", "eventId"));

        return e;
    }

}