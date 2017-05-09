package biocode.fims.validation;

import biocode.fims.digester.Attribute;
import biocode.fims.digester.DataType;
import biocode.fims.digester.Entity;
import biocode.fims.digester.Mapping;
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
public class GenericRecordValidatorTest {

    RecordValidator validator;

    @Before
    public void setUp() {
        this.validator = new RecordValidator();
        this.validator.setProjectConfig(config());
    }

    @Test
    public void should_throw_exception_if_project_config_is_null_in_validate() {
        try {
            validator.setProjectConfig(null);
            validator.validate(null);
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
        assertTrue(validator.validate(new RecordSet(entity1())));
        assertEquals(validator.messages(), new EntityMessages(entity1().getConceptAlias(), entity1().getWorksheet()));
    }

    @Test
    public void should_add_default_rules_to_record_set() {
        validator.setProjectConfig(config());
        RecordSet recordSet = new RecordSet(entity1());

        Record r1 = new GenericRecord();
        r1.set("eventId", "1234");
        recordSet.add(r1);

        Record r2 = new GenericRecord();
        r2.set("eventId", "1234");
        r2.set("col2", "-10");
        recordSet.add(r2);

        Record r3 = new GenericRecord();
        r3.set("eventId", "nonUri=");
        r3.set("col2", "nonInteger");
        recordSet.add(r3);

        Record r4 = new GenericRecord();
        r4.set("eventId", "");
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

        assertEquals(expectedMessages, validator.messages());


    }

    private Entity entity1() {
        Entity e = new Entity("event");

        e.setUniqueKey("eventId");
        e.setWorksheet("events");

        e.addAttribute(new Attribute("eventId", "eventId"));
        e.addAttribute(new Attribute("col1", "col1"));
        Attribute a1 = new Attribute("col2", "col2");
        a1.setDatatype(DataType.INTEGER);
        e.addAttribute(a1);

        Rule rule = new NumericRangeRule("col2", ">0|<=10");
        e.addRule(rule);

        return e;
    }

    private ProjectConfig config() {
        Mapping mapping = new Mapping();
        mapping.addEntity(entity1());

        return new ProjectConfig(mapping, null, null);
    }

}