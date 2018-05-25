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

import static biocode.fims.bcid.Identifier.ROOT_IDENTIFIER;
import static org.junit.Assert.*;

/**
 * @author rjewing
 */
public class QueryResultTest {

    @Test
    public void should_build_bcid_without_parent_entity() {

        List<Record> records = new ArrayList<>();
        Record r1 = new GenericRecord();
        r1.set("urn:eventId", "1");
        r1.set(ROOT_IDENTIFIER, "ark:/99999/l2");

        records.add(r1);

        QueryResult result = new QueryResult(records, event());

        List<Map<String, String>> expected = new ArrayList<>();
        Map<String, String> expectedR1 = new HashMap<>();
        expectedR1.put("eventId", "1");
        expectedR1.put("bcid", "ark:/99999/l21");
        expected.add(expectedR1);

        assertEquals(expected, result.get(true));

    }

    @Test
    public void should_transform_uris_to_columns() {

        List<Record> records = new ArrayList<>();

        Record r1 = new GenericRecord();
        r1.set("urn:sampleId", "1");
        r1.set("urn:eventId", "1");
        r1.set("urn:col3", "value");
        r1.set("urn:col4", "another");

        records.add(r1);

        QueryResult result = new QueryResult(records, sample(), event());

        List<Map<String, String>> expected = new ArrayList<>();
        Map<String, String> expectedR1 = new HashMap<>();
        expectedR1.put("sampleId", "1");
        expectedR1.put("eventId", "1");
        expectedR1.put("col3", "value");
        expectedR1.put("col4", "another");
        expectedR1.put("bcid", "1_1");
        expected.add(expectedR1);

        assertEquals(expected, result.get(true));

    }

    @Test
    public void should_include_all_entity_columns() {

        List<Record> records = new ArrayList<>();

        Record r1 = new GenericRecord();
        r1.set("urn:sampleId", "1");
        r1.set("urn:eventId", "1");
        r1.set("urn:col3", "value");

        records.add(r1);

        QueryResult result = new QueryResult(records, sample(), event());

        List<Map<String, String>> expected = new ArrayList<>();
        Map<String, String> expectedR1 = new HashMap<>();
        expectedR1.put("sampleId", "1");
        expectedR1.put("eventId", "1");
        expectedR1.put("col3", "value");
        expectedR1.put("col4", "");
        expectedR1.put("bcid", "1_1");
        expected.add(expectedR1);

        assertEquals(expected, result.get(true));

    }

    private Entity event() {
        Entity e = new Entity("event", "uri:event");
        e.setUniqueKey("eventId");
        e.addAttribute(new Attribute("eventId", "urn:eventId"));
        return e;
    }

    private Entity sample() {
        Entity e = new Entity("sample", "someURI");
        e.setParentEntity("event");
        e.setUniqueKey("sampleId");
        e.addAttribute(new Attribute("sampleId", "urn:sampleId"));
        e.addAttribute(new Attribute("eventId", "urn:eventId"));
        e.addAttribute(new Attribute("col3", "urn:col3"));
        e.addAttribute(new Attribute("col4", "urn:col4"));
        return e;
    }

}