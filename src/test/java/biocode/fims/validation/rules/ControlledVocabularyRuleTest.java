package biocode.fims.validation.rules;

import biocode.fims.digester.Field;
import biocode.fims.digester.List;
import biocode.fims.models.records.GenericRecord;
import biocode.fims.models.records.Record;
import biocode.fims.models.records.RecordSet;
import biocode.fims.projectConfig.ProjectConfig;
import biocode.fims.renderers.EntityMessages;
import biocode.fims.renderers.SimpleMessage;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * @author rjewing
 */
public class ControlledVocabularyRuleTest extends AbstractRuleTest {

    @Test(expected = IllegalArgumentException.class)
    public void should_throw_exception_for_null_recordSet() {
        Rule rule = new ControlledVocabularyRule(null, "", config(), RuleLevel.WARNING);
        assertTrue(rule.run(null, messages));
    }

    @Test(expected = IllegalStateException.class)
    public void should_throw_exception_for_null_config() {
        Rule rule = new ControlledVocabularyRule("col1", "", null);
        assertTrue(rule.run(new RecordSet(entity()), messages));
    }

    @Test
    public void should_be_valid_for_empty_recordSet() {
        Rule rule = new ControlledVocabularyRule("col1", "yesNo", config());

        assertTrue(rule.run(new RecordSet(entity()), messages));
    }

    @Test
    public void should_not_validate_if_empty_column() {
        Rule rule = new ControlledVocabularyRule(null, "yesNo", config());

        assertFalse(rule.run(new RecordSet(entity()), messages));
        assertTrue(rule.hasError());

        EntityMessages expectedMessages = new EntityMessages("Samples");
        expectedMessages.addErrorMessage(
                "Invalid Rule Configuration. Contact Project Administrator.",
                new SimpleMessage("Invalid ControlledVocabulary Rule configuration. Column must not be blank or null.")
        );

        assertEquals(expectedMessages, messages);
    }

    @Test
    public void should_not_validate_if_no_listName() {
        Rule rule = new ControlledVocabularyRule("col1", null, config());

        assertFalse(rule.run(new RecordSet(entity()), messages));
        assertTrue(rule.hasError());

        EntityMessages expectedMessages = new EntityMessages("Samples");
        expectedMessages.addErrorMessage(
                "Invalid Rule Configuration. Contact Project Administrator.",
                new SimpleMessage("Invalid ControlledVocabulary Rule configuration. listName must not be blank or null.")
        );

        assertEquals(expectedMessages, messages);
    }

    @Test
    public void should_not_validate_if_cant_find_list() {
        Rule rule = new ControlledVocabularyRule("col1", "list1", config());

        assertFalse(rule.run(new RecordSet(entity()), messages));
        assertTrue(rule.hasError());

        EntityMessages expectedMessages = new EntityMessages("Samples");
        expectedMessages.addErrorMessage(
                "Invalid Rule Configuration. Contact Project Administrator.",
                new SimpleMessage("Invalid Project configuration. Could not find list with name \"list1\"")
        );

        assertEquals(expectedMessages, messages);
    }

    @Test
    public void should_not_be_valid_when_value_not_in_case_sensitive_list() {

        Rule rule = new ControlledVocabularyRule("col1", "yesNo", config());

        RecordSet recordSet = new RecordSet(entity());

        Record r1 = new GenericRecord();
        r1.set("urn:col1", "Yes");
        Record r2 = new GenericRecord();
        r2.set("urn:col1", "yes");
        recordSet.add(r1);
        recordSet.add(r2);

        assertFalse(rule.run(recordSet, messages));

        EntityMessages expectedMessages = new EntityMessages("Samples");
        expectedMessages.addWarningMessage(
                "\"col1\" contains value not in list \"yesNo\"",
                new SimpleMessage("\"Yes\" not an approved \"col1\""
                )
        );

        assertEquals(expectedMessages, messages);
    }

    @Test
    public void should_not_be_valid_when_value_not_in_case_insensitive_list() {

        Rule rule = new ControlledVocabularyRule("col1", "trueFalse", config());

        RecordSet recordSet = new RecordSet(entity());

        Record r1 = new GenericRecord();
        r1.set("urn:col1", "True");
        Record r2 = new GenericRecord();
        r2.set("urn:col1", "true");
        Record r3 = new GenericRecord();
        r3.set("urn:col1", "error");
        Record r4 = new GenericRecord();
        r4.set("urn:col1", "another");
        Record r5 = new GenericRecord();
        r5.set("urn:col1", "");
        recordSet.add(r1);
        recordSet.add(r2);
        recordSet.add(r3);
        recordSet.add(r4);
        recordSet.add(r5);

        assertFalse(rule.run(recordSet, messages));

        EntityMessages expectedMessages = new EntityMessages("Samples");
        expectedMessages.addWarningMessage(
                "\"col1\" contains value not in list \"trueFalse\"",
                new SimpleMessage("\"error\" not an approved \"col1\""
                )
        );
        expectedMessages.addWarningMessage(
                "\"col1\" contains value not in list \"trueFalse\"",
                new SimpleMessage("\"another\" not an approved \"col1\""
                )
        );

        assertEquals(expectedMessages, messages);
    }


    private ProjectConfig config() {
        ProjectConfig config = new ProjectConfig();

        List l1 = new List();
        l1.setAlias("yesNo");
        Field f1 = new Field();
        f1.setValue("yes");
        l1.addField(f1);
        Field f2 = new Field();
        f2.setValue("no");
        l1.addField(f2);

        List l2 = new List();
        l2.setCaseInsensitive(true);
        l2.setAlias("trueFalse");
        Field f3 = new Field();
        f3.setValue("true");
        l2.addField(f3);
        Field f4 = new Field();
        f4.setValue("false");
        l2.addField(f4);

        config.addList(l1);
        config.addList(l2);

        return config;
    }
}