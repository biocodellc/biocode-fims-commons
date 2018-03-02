package biocode.fims.query;

import biocode.fims.digester.Attribute;
import biocode.fims.digester.Entity;
import biocode.fims.models.records.GenericRecord;
import biocode.fims.models.records.Record;
import org.junit.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;

/**
 * @author rjewing
 */
public class QueryResultTest {

    @Test
    public void should_transform_uris_to_columns() {

        List<Record> records = new ArrayList<>();

        Record r1 = new GenericRecord();
        r1.set("urn:sampleId", "1");
        r1.set("sample_eventId", "1");
        r1.set("urn:col3", "value");
        r1.set("urn:col4", "another");

        records.add(r1);

        QueryResult result = new QueryResult(records, sample(), null);

        List<Map<String, String>> expected = new ArrayList<>();
        Map<String, String> expectedR1 = new HashMap<>();
        expectedR1.put("sampleId", "1");
        expectedR1.put("eventId", "1");
        expectedR1.put("col3", "value");
        expectedR1.put("col4", "another");
        expected.add(expectedR1);

        assertEquals(expected, result.get(true));

    }

    @Test
    public void should_include_all_entity_columns() {

        List<Record> records = new ArrayList<>();

        Record r1 = new GenericRecord();
        r1.set("urn:sampleId", "1");
        r1.set("sample_eventId", "1");
        r1.set("urn:col3", "value");

        records.add(r1);

        QueryResult result = new QueryResult(records, sample(), null);

        List<Map<String, String>> expected = new ArrayList<>();
        Map<String, String> expectedR1 = new HashMap<>();
        expectedR1.put("sampleId", "1");
        expectedR1.put("eventId", "1");
        expectedR1.put("col3", "value");
        expectedR1.put("col4", "");
        expected.add(expectedR1);

        assertEquals(expected, result.get(true));

    }

    private Entity sample() {
        Entity e = new Entity("sample", "someURI");
        e.setParentEntity("event");
        e.setUniqueKey("sampleId");
        e.addAttribute(new Attribute("sampleId", "urn:sampleId"));
        e.addAttribute(new Attribute("eventId", "sample_eventId"));
        e.addAttribute(new Attribute("col3", "urn:col3"));
        e.addAttribute(new Attribute("col4", "urn:col4"));
        return e;
    }

}