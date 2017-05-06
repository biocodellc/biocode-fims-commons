package biocode.fims.validation.rules;

import biocode.fims.models.records.GenericRecord;
import biocode.fims.models.records.Record;
import biocode.fims.models.records.RecordSet;
import biocode.fims.renderers.EntityMessages;
import biocode.fims.renderers.SimpleMessage;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.*;

/**
 * @author rjewing
 */
public class ValidForURIRuleTest extends AbstractRuleTest {

    private ValidForURIRule rule;

    @Before
    public void setUp() {
        this.rule = new ValidForURIRule();
    }

    @Test(expected = IllegalArgumentException.class)
    public void should_throw_exception_for_null_recordSet() {
        assertTrue(rule.run(null, messages));
    }

    @Test
    public void should_be_valid_for_empty_recordSet() {
        assertTrue(rule.run(new RecordSet(entity()), messages));
    }

    @Test
    public void should_not_be_valid_for_any_invalid_URI_chars() {
        RecordSet recordSet = new RecordSet(entity());

        List<Record> invalidRecords = getInvalidRecords();
        invalidRecords.forEach(recordSet::add);

        List<Record> validRecords = getValidRecords();
        validRecords.forEach(recordSet::add);

        rule.setColumn("col1");
        rule.setLevel(RuleLevel.ERROR);

        assertFalse(rule.run(recordSet, messages));
        assertTrue(rule.hasError());

        EntityMessages expectedMessages = new EntityMessages("Samples");
        expectedMessages.addErrorMessage(
                "Non-valid URI characters",
                new SimpleMessage("\"col1\" contains some invalid URI characters: \"" + String.join("\", \"", getInvalidRecordValues()) + "\"")
        );

        assertEquals(expectedMessages, messages);

    }

    private List<Record> getValidRecords() {
        List<Record> recordSet = new ArrayList<>();

        Record r1 = new GenericRecord();
        r1.set("urn:col1", "validUri");
        Record r2 = new GenericRecord();
        r2.set("urn:col1", "123AOl");
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
                "test+",
                "test,",
                "test/",
                "test:",
                "test;",
                "test=",
                "test?",
                "test@",
                "test<",
                "test>",
                "test#",
                "test\\"
        );
    }
}