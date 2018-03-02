package biocode.fims.models.records;

import biocode.fims.digester.Attribute;
import biocode.fims.digester.Entity;
import biocode.fims.fimsExceptions.FimsRuntimeException;
import biocode.fims.fimsExceptions.errorCodes.DataReaderCode;
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
        RecordSet recordSet = new RecordSet(entity(), false);

        assertTrue(recordSet.isEmpty());
        assertEquals("resource", recordSet.conceptAlias());
    }

    @Test
    public void has_records() {
        RecordSet recordSet = new RecordSet(entity(), false);
        Record r = record1();
        recordSet.add(r);

        assertFalse(recordSet.isEmpty());
        assertEquals(r, recordSet.records().get(0));
    }

    @Test
    public void has_records_from_constructor() {
        List<Record> records = Collections.singletonList(record1());

        RecordSet recordSet = new RecordSet(entity(), records, false);

        assertFalse(recordSet.isEmpty());
        assertEquals(records, recordSet.records());
    }

    @Test
    public void should_add_new_records_when_merge() {
        List<Record> startingRecords = Collections.singletonList(record1());

        RecordSet recordSet = new RecordSet(entity(), startingRecords, false);
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

        RecordSet recordSet = new RecordSet(entity(), startingRecords, false);
        recordSet.merge(Collections.singletonList(record1()));

        assertEquals(1, recordSet.records().size());
        assertEquals(startingRecords, recordSet.records());
    }

    @Test
    public void should_throw_exception_when_removeDuplicates_and_records_with_same_identifier_but_different_properties() {
        List<Record> records = new ArrayList<>();
        records.add(record1());
        Record r2 = record1();
        r2.set("urn:column2", "another value");
        records.add(r2);

        RecordSet recordSet = new RecordSet(entity(), records, false);

        try {
            recordSet.removeDuplicates();
            fail();
        } catch (FimsRuntimeException e) {
            assertEquals(DataReaderCode.INVALID_RECORDS, e.getErrorCode());
        }
    }

    @Test
    public void should_remove_duplicate_records_with_same_properties() {
        List<Record> records = new ArrayList<>();
        records.add(record1());
        records.add(record1());
        records.add(record2());

        RecordSet recordSet = new RecordSet(entity(), records, false);

        recordSet.removeDuplicates();

        List<Record> deduped = recordSet.records();
        assertEquals(2, deduped.size());

        List<String> ids = Arrays.asList("1", "2");
        for (Record r: deduped) {
            assertTrue(ids.contains(r.get("urn:column1")));
        }
    }

    private Record record1() {
        GenericRecord r = new GenericRecord();
        r.set("urn:column1", "1");
        r.set("urn:column2", "value");
        return r;
    }

    private Record record2() {
        GenericRecord r = new GenericRecord();
        r.set("urn:column1", "2");
        return r;
    }

    private Entity entity() {
        Entity entity = new Entity("resource", "someURI");
        entity.setUniqueKey("column1");

        entity.addAttribute(new Attribute("column1", "urn:column1"));
        entity.addAttribute(new Attribute("column2", "urn:column2"));

        return entity;
    }

}