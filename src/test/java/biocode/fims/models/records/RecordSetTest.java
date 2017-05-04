package biocode.fims.models.records;

import biocode.fims.digester.Attribute;
import biocode.fims.digester.Entity;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.*;

/**
 * @author rjewing
 */
public class RecordSetTest {

    @Test
    public void is_empty_with_no_records() {
        RecordSet recordSet = new RecordSet(entity());

        assertTrue(recordSet.isEmpty());
        assertEquals("resource", recordSet.conceptAlias());
    }

    @Test
    public void has_records() {
        RecordSet recordSet = new RecordSet(entity());
        Record r = record1();
        recordSet.add(r);

        assertFalse(recordSet.isEmpty());
        assertEquals(r, recordSet.records().get(0));
    }

    @Test
    public void has_records_from_constructor() {
        List<Record> records = Collections.singletonList(record1());

        RecordSet recordSet = new RecordSet(entity(), records);

        assertFalse(recordSet.isEmpty());
        assertEquals(records, recordSet.records());
    }

    @Test
    public void should_add_new_records_when_merge() {
        List<Record> startingRecords = Collections.singletonList(record1());

        RecordSet recordSet = new RecordSet(entity(), startingRecords);
        recordSet.merge(Collections.singletonList(record2()));

        assertEquals(2, recordSet.records().size());

        String uniqueKey = entity().getUniqueKeyURI();

        List<Record> records = recordSet.records();
        assertEquals(1,
                records.stream()
                        .filter(
                                r -> r.get(uniqueKey).equals(record1().get(uniqueKey))
                        ).count()
        );

        assertEquals(1,
                records.stream()
                        .filter(
                                r -> r.get(uniqueKey).equals(record2().get(uniqueKey))
                        ).count()
        );
    }

    @Test
    public void should_not_add_duplicate_records_when_merge() {
        List<Record> startingRecords = Collections.singletonList(record1());

        RecordSet recordSet = new RecordSet(entity(), startingRecords);
        recordSet.merge(Collections.singletonList(record1()));

        assertEquals(1, recordSet.records().size());
        assertEquals(startingRecords, recordSet.records());
    }

    private Record record1() {
        GenericRecord r = new GenericRecord();
        r.set("urn:column1", "1");
        return r;
    }

    private Record record2() {
        GenericRecord r = new GenericRecord();
        r.set("urn:column1", "2");
        return r;
    }

    private Entity entity() {
        Entity entity = new Entity();
        entity.setConceptAlias("resource");
        entity.setUniqueKey("column1");

        Attribute a = new Attribute("column1", "urn:column1");
        entity.addAttribute(a);
        return entity;
    }

}