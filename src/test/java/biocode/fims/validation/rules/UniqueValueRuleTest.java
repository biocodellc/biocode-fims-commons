package biocode.fims.validation.rules;

import biocode.fims.digester.Attribute;
import biocode.fims.digester.Entity;
import biocode.fims.models.records.GenericRecord;
import biocode.fims.models.records.Record;
import biocode.fims.models.records.RecordSet;
import biocode.fims.renderers.MessagesGroup;
import biocode.fims.renderers.SimpleMessage;
import org.junit.*;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.Assert.*;

/**
 * @author rjewing
 */
public class UniqueValueRuleTest {

    private UniqueValueRule rule;

    @Before
    public void setUp() {
        this.rule = new UniqueValueRule();
    }

    @org.junit.Test(expected = IllegalArgumentException.class)
    public void should_throw_exception_for_null_recordSet() {
        assertTrue(rule.run(null));
    }

    @org.junit.Test
    public void should_be_valid_for_empty_recordSet() {
        assertTrue(rule.run(new RecordSet(entity())));
    }

    @Test
    public void should_not_be_valid_when_duplicate_values() {
        RecordSet recordSet = new RecordSet(entity());

        List<Record> invalidRecords = getInvalidRecords();
        invalidRecords.forEach(recordSet::add);

        List<Record> validRecords = getValidRecords();
        validRecords.forEach(recordSet::add);

        rule.setColumn("col1");
        rule.setLevel(RuleLevel.ERROR);

        assertFalse(rule.run(recordSet));
        assertEquals(RuleLevel.ERROR, rule.level());

        MessagesGroup messages = new MessagesGroup("Unique value constraint did not pass");
        messages.add(new SimpleMessage(
                "\"col1\" column is defined as unique but some values used more than once: \"" +
                        String.join("\", \"", getInvalidRecordValues().stream().distinct().collect(Collectors.toList())) + "\""
        ));

        assertEquals(messages, rule.messages());
    }

    private List<Record> getValidRecords() {
        List<Record> recordSet = new ArrayList<>();

        Record r1 = new GenericRecord();
        r1.set("urn:col1", "");
        Record r2 = new GenericRecord();
        r2.set("urn:col1", "value3");
        Record r3 = new GenericRecord();
        r3.set("urn:col1", "");
        return recordSet;
    }

    private List<Record> getInvalidRecords() {
        List<Record> recordSet = new ArrayList<>();

        for (String v: getInvalidRecordValues()) {
            Record r = new GenericRecord();
            r.set("urn:col1", v);
            recordSet.add(r);
        }

        return recordSet;
    }

    private List<String> getInvalidRecordValues() {
        return Arrays.asList(
                "value1",
                "value1",
                "value2",
                "value2"
        );
    }

    private Entity entity() {
        Entity entity = new Entity();
        entity.setConceptAlias("Samples");

        Attribute a = new Attribute("col1", "urn:col1");
        entity.addAttribute(a);

        return entity;
    }

}