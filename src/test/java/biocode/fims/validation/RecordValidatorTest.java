package biocode.fims.validation;

import biocode.fims.records.GenericRecord;
import biocode.fims.records.Record;
import biocode.fims.records.RecordSet;
import biocode.fims.projectConfig.ProjectConfig;
import biocode.fims.projectConfig.models.*;
import biocode.fims.validation.messages.EntityMessages;
import biocode.fims.validation.messages.Message;
import biocode.fims.validation.rules.ControlledVocabularyRule;
import biocode.fims.validation.rules.NumericRangeRule;
import biocode.fims.validation.rules.Rule;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * @author rjewing
 */
public class RecordValidatorTest {

    RecordValidator validator;

    @Before
    public void setUp() {
        ProjectConfig config = config();
        config.addEntity(entity2());
        this.validator = new RecordValidator(config);
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
        RecordSet recordSet = new RecordSet(entity1(), false);
        recordSet.setParent(new RecordSet(entity2(), false));

        assertTrue(validator.validate(recordSet));
        assertEquals(validator.messages(), new EntityMessages(entity1().getConceptAlias(), entity1().getWorksheet()));
    }

    // TODO re-enable this test
    @Ignore
    @Test
    public void should_add_default_rules_to_record_set() {
        RecordSet parent = new RecordSet(entity2(), false);

        Record p1 = new GenericRecord();
        p1.set("parentId", "parent1");
        parent.add(p1);

        RecordSet recordSet = new RecordSet(entity1(), false);
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
                new Message("Value \"-10\" out of range for \"col2\" using range validation = \">0|<=10\"")
        );
        expectedMessages.addWarningMessage(
                "Invalid number format",
                new Message("Value \"nonInteger\" out of range for \"col2\" using range validation = \">0|<=10\"")
        );
        expectedMessages.addErrorMessage(
                "Invalid DataFormat",
                new Message("\"col2\" contains non-integer value \"nonInteger\"")
        );
        expectedMessages.addErrorMessage(
                "Non-valid URI characters",
                new Message("\"eventId\" contains some invalid URI characters: \"\"")
        );
        expectedMessages.addErrorMessage(
                "Missing column(s)",
                new Message("\"parentId\" has a missing cell value")
        );
        expectedMessages.addErrorMessage(
                "Missing column(s)",
                new Message("\"eventId\" has a missing cell value")
        );
        expectedMessages.addErrorMessage(
                "Unique value constraint did not pass",
                new Message("(\"parentId\", \"eventId\") is defined as a composite unique key, but some value combinations were used more than once: (\"parent1\", \"1234\")")
        );
        expectedMessages.addErrorMessage(
                "Invalid parent identifier(s)",
                new Message("The following identifiers do not exist in the parent entity \"parent\": [\"\", \"parent2\"]")
        );

        assertEquals(expectedMessages, validator.messages());


    }

    @Test
    public void should_call_rule_setConfig_before_running_rule() {
        RecordSet recordSet = new RecordSet(entity2(), false);

        Record p1 = new GenericRecord();
        p1.set("parentId", "parent1");
        p1.set("col1", "YES");
        p1.setExpeditionCode("exp");
        p1.setProjectId(1);
        recordSet.add(p1);

        assertTrue(validator.validate(recordSet));
    }

    private Entity entity1() {
        Entity e = new Entity("event", "someURI");

        e.setUniqueKey("eventId");
        e.setWorksheet("events");
        e.setParentEntity("parent");

        e.addAttribute(new Attribute("eventId", "eventId"));
        e.addAttribute(new Attribute("col1", "col1"));
        e.addAttribute(new Attribute("parentId", "parentId"));
        Attribute a1 = new Attribute("col2", "col2");
        a1.setDataType(DataType.INTEGER);
        e.addAttribute(a1);

        Rule rule = new NumericRangeRule("col2", ">0|<=10");
        e.addRule(rule);

        return e;
    }

    private Entity entity2() {
        Entity e = new Entity("parent", "someURI");
        e.setUniqueKey("parentId");

        e.addAttribute(new Attribute("parentId", "parentId"));
        e.addAttribute(new Attribute("col1", "col1"));

        Rule rule = new ControlledVocabularyRule("col1", "yesNo", config());
        e.addRule(rule);

        return e;
    }

    private ProjectConfig config() {
        ProjectConfig config = new ProjectConfig();
        config.addEntity(entity1());

        List yesNoList = new List();
        yesNoList.setAlias("yesNo");
        Field f1 = new Field();
        f1.setValue("yes");
        yesNoList.addField(f1);
        Field f2 = new Field();
        f2.setValue("no");
        yesNoList.addField(f2);
        yesNoList.setCaseInsensitive(true);

        config.addList(yesNoList);

        return config;
    }

}