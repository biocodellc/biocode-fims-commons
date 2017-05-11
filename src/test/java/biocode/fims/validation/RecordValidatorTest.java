package biocode.fims.validation;

import biocode.fims.digester.Attribute;
import biocode.fims.digester.DataType;
import biocode.fims.digester.Entity;
import biocode.fims.digester.Mapping;
import biocode.fims.models.Project;
import biocode.fims.models.records.GenericRecord;
import biocode.fims.models.records.Record;
import biocode.fims.models.records.RecordSet;
import biocode.fims.projectConfig.ProjectConfig;
import biocode.fims.renderers.EntityMessages;
import biocode.fims.renderers.SimpleMessage;
import biocode.fims.validation.rules.NumericRangeRule;
import biocode.fims.validation.rules.Rule;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * @author rjewing
 */
public class RecordValidatorTest {

    RecordValidator validator;

    @Before
    public void setUp() {
        this.validator = new RecordValidator(config());
    }

    @Test
    public void should_throw_exception_if_project_config_is_null_in_validate() {
        try {
            new RecordValidator(null).validate(null);
            fail();
        } catch (IllegalArgumentException e) {
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void should_throw_exception_if_recordSet_is_null_in_validate() {
        validator.validate(null);
    }

    @Test
    public void should_validate_and_have_empty_messages_with_empty_record_set() {
        RecordSet recordSet = new RecordSet(entity1());
        recordSet.setParent(new RecordSet(entity2()));

        assertTrue(validator.validate(recordSet));
        assertEquals(validator.messages(), new EntityMessages(entity1().getConceptAlias(), entity1().getWorksheet()));
    }

    @Test
    public void should_add_default_rules_to_record_set() {
        RecordSet parent = new RecordSet(entity2());

        Record p1 = new GenericRecord();
        p1.set("parentId", "parent1");
        parent.add(p1);

        RecordSet recordSet = new RecordSet(entity1());
        recordSet.setParent(parent);

        Record r1 = new GenericRecord();
        r1.set("eventId", "1234");
        r1.set("parentId", "parent1");
        recordSet.add(r1);

        Record r2 = new GenericRecord();
        r2.set("eventId", "1234");
        r2.set("parentId", "parent1");
        r2.set("col2", "-10");
        recordSet.add(r2);

        Record r3 = new GenericRecord();
        r3.set("eventId", "nonUri=");
        r3.set("col2", "nonInteger");
        recordSet.add(r3);

        Record r4 = new GenericRecord();
        r4.set("eventId", "");
        r4.set("parentId", "parent2");
        recordSet.add(r4);

        assertFalse(validator.validate(recordSet));
        assertTrue(validator.hasError());

        EntityMessages expectedMessages = new EntityMessages("event", "events");
        expectedMessages.addWarningMessage(
                "Invalid number format",
                new SimpleMessage("Value \"-10\" out of range for \"col2\" using range validation = \">0|<=10\"")
        );
        expectedMessages.addWarningMessage(
                "Invalid number format",
                new SimpleMessage("Value \"nonInteger\" out of range for \"col2\" using range validation = \">0|<=10\"")
        );
        expectedMessages.addErrorMessage(
                "Invalid DataFormat",
                new SimpleMessage("\"col2\" contains non-integer value \"nonInteger\"")
        );
        expectedMessages.addErrorMessage(
                "Unique value constraint did not pass",
                new SimpleMessage("\"eventId\" column is defined as unique but some values used more than once: \"1234\"")
        );
        expectedMessages.addErrorMessage(
                "Non-valid URI characters",
                new SimpleMessage("\"eventId\" contains some invalid URI characters: \"nonUri=\", \"\"")
        );
        expectedMessages.addErrorMessage(
                "Missing column(s)",
                new SimpleMessage("\"eventId\" has a missing cell value")
        );
        expectedMessages.addErrorMessage(
                "Invalid parent identifier(s)",
                new SimpleMessage("The following identifiers do not exist in the parent entity \"parent\": [\"\", \"parent2\"]")
        );

        assertEquals(expectedMessages, validator.messages());


    }

    private Entity entity1() {
        Entity e = new Entity("event");

        e.setUniqueKey("eventId");
        e.setWorksheet("events");
        e.setParentEntity("parent");

        e.addAttribute(new Attribute("eventId", "eventId"));
        e.addAttribute(new Attribute("col1", "col1"));
        e.addAttribute(new Attribute("parentId", "parentId"));
        Attribute a1 = new Attribute("col2", "col2");
        a1.setDatatype(DataType.INTEGER);
        e.addAttribute(a1);

        Rule rule = new NumericRangeRule("col2", ">0|<=10");
        e.addRule(rule);

        return e;
    }

    private Entity entity2() {
        Entity e = new Entity("parent");
        e.setUniqueKey("parentId");

        e.addAttribute(new Attribute("parentId", "parentId"));
        return e;
    }

    private ProjectConfig config() {
        ProjectConfig config = new ProjectConfig();
        config.addEntity(entity1());
        config.addEntity(entity2());

        return config;
    }

}