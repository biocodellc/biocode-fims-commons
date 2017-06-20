package biocode.fims.validation.rules;

import biocode.fims.models.records.GenericRecord;
import biocode.fims.models.records.Record;
import biocode.fims.models.records.RecordSet;
import biocode.fims.validation.messages.EntityMessages;
import biocode.fims.validation.messages.Message;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.*;

/**
 * @author rjewing
 */
public class ValidForURIRuleTest extends AbstractRuleTest {

    @Test(expected = IllegalArgumentException.class)
    public void should_throw_exception_for_null_recordSet() {
        Rule rule = new ValidForURIRule(null);
        assertTrue(rule.run(null, messages));
    }

    @Test
    public void should_be_valid_for_empty_recordSet() {
        Rule rule = new ValidForURIRule("col1");
        assertTrue(rule.run(new RecordSet(entity()), messages));
    }

    @Test
    public void should_not_validate_if_null_column() {
        Rule rule = new ValidForURIRule(null);

        assertFalse(rule.run(new RecordSet(entity()), messages));
        assertTrue(rule.hasError());

        EntityMessages expectedMessages = new EntityMessages("Samples");
        expectedMessages.addErrorMessage(
                "Invalid Rule Configuration. Contact Project Administrator.",
                new Message("Invalid ValidForURI Rule configuration. Column must not be blank or null.")
        );

        assertEquals(expectedMessages, messages);
    }

    @Test
    public void should_not_be_valid_for_any_invalid_URI_chars() {
        Rule rule = new ValidForURIRule("col1", RuleLevel.ERROR);
        RecordSet recordSet = new RecordSet(entity());

        List<Record> invalidRecords = getInvalidRecords();
        invalidRecords.forEach(recordSet::add);

        List<Record> validRecords = getValidRecords();
        validRecords.forEach(recordSet::add);

        assertFalse(rule.run(recordSet, messages));
        assertTrue(rule.hasError());

        EntityMessages expectedMessages = new EntityMessages("Samples");
        expectedMessages.addErrorMessage(
                "Non-valid URI characters",
                new Message("\"col1\" contains some invalid URI characters: \"" + String.join("\", \"", getInvalidRecordValues()) + "\"")
        );

        assertEquals(expectedMessages, messages);

    }

    private List<Record> getValidRecords() {
        List<Record> recordSet = new ArrayList<>();

        Record r1 = new GenericRecord();
        r1.set("urn:col1", "validUri");
        Record r2 = new GenericRecord();
        r2.set("urn:col1", "123AOl+=:._(~*)");
        Record r3 = new GenericRecord();
        r3.set("urn:col1", "");
        return recordSet;
    }

    private List<Record> getInvalidRecords() {
        List<Record> recordSet = new ArrayList<>();

        for (String v : getInvalidRecordValues()) {
            Record r = new GenericRecord();
            r.set("urn:col1", v);
            recordSet.add(r);
        }

        return recordSet;
    }

    private List<String> getInvalidRecordValues() {
        return Arrays.asList(
                "tes t",
                "test%",
                "test$",
                "test&",
                "test,",
                "test/",
                "test;",
                "test?",
                "test@",
                "test-",
                "test<",
                "test>",
                "test#",
                "test\\"
        );
    }
}