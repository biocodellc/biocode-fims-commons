package biocode.fims.models.records;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

/**
 * @author rjewing
 */
public class RecordSetTest {

    @Test
    public void is_empty_with_no_records() {
        RecordSet recordSet = new RecordSet("resource");

        assertTrue(recordSet.isEmpty());
        assertEquals("resource", recordSet.conceptAlias());
    }

    @Test
    public void has_records() {
        RecordSet recordSet = new RecordSet("resource");
        GenericRecord r = new GenericRecord();
        r.set("prop1", "true");
        recordSet.add(r);

        assertFalse(recordSet.isEmpty());
        assertEquals(r, recordSet.records().get(0));
    }

    @Test
    public void has_records_from_constructor() {
        List<Record> records = new ArrayList();
        GenericRecord r = new GenericRecord();
        r.set("prop1", "true");
        records.add(r);

        RecordSet recordSet = new RecordSet("resource", records);

        assertFalse(recordSet.isEmpty());
        assertEquals(records, recordSet.records());
    }

}