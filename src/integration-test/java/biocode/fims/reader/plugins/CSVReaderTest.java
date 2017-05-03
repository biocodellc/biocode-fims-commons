package biocode.fims.reader.plugins;

import biocode.fims.digester.Attribute;
import biocode.fims.digester.Entity;
import biocode.fims.digester.Mapping;
import biocode.fims.fimsExceptions.FimsRuntimeException;
import biocode.fims.fimsExceptions.errorCodes.DataReaderCode;
import biocode.fims.models.records.GenericRecord;
import biocode.fims.models.records.Record;
import biocode.fims.models.records.RecordMetadata;
import biocode.fims.models.records.RecordSet;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * @author rjewing
 */
public class CSVReaderTest {

    private ClassLoader classLoader;

    @Before
    public void setUp() throws Exception {
        if (classLoader == null) {
            classLoader = getClass().getClassLoader();
        }
    }

    @Test
    public void test_not_null_assertions() {
        try {
            new CSVReader(null, null, null);
            fail();
        } catch (IllegalArgumentException e) {
        }

        try {
            new CSVReader(new File("test.csv"), null, null);
            fail();
        } catch (IllegalArgumentException e) {
        }

        try {
            new CSVReader(new File("test.csv"), new Mapping(), null);
            fail();
        } catch (IllegalArgumentException e) {
        }

    }

    @Test
    public void should_return_all_records_for_single_entity_mappping() {
        File csvFile = new File(classLoader.getResource("testDataset.csv").getFile());

        RecordMetadata rm = new RecordMetadata(GenericRecord.class);
        rm.add(CSVReader.SHEET_NAME_KEY, "sheet1");
        DataReader reader = new CSVReader(csvFile, getSingleEntityMapping(), rm);

        List<RecordSet> recordSets = reader.getRecordSets();

        assertEquals(1, recordSets.size());

        for (RecordSet set: recordSets) {
            assertEquals("samples", set.conceptAlias());
            assertEquals(4, set.records().size());

            Record r = set.records().get(0);

            assertTrue(r.has("urn:materialSampleID"));
            assertTrue(r.has("urn:principalInvestigator"));
            assertTrue(r.has("urn:locality"));
            assertTrue(r.has("urn:decimalLatitude"));
            assertTrue(r.has("urn:decimalLongitude"));
            assertTrue(r.has("urn:ObservationDate"));
            assertTrue(r.has("urn:timeCollected"));
            assertTrue(r.has("urn:collectionTimestamp"));
            assertTrue(r.has("urn:elevation"));
            assertTrue(r.has("urn:hasLegs"));
        }
    }

    @Test
    public void should_return_all_records_for_multiple_entity_mappping() {
        File csvFile = new File(classLoader.getResource("testDataset.csv").getFile());

        RecordMetadata rm = new RecordMetadata(GenericRecord.class);
        rm.add(CSVReader.SHEET_NAME_KEY, "sheet1");
        DataReader reader = new CSVReader(csvFile, getMultipleEntityMapping(), rm);

        List<RecordSet> recordSets = reader.getRecordSets();

        assertEquals(2, recordSets.size());

        for (RecordSet set: recordSets) {
            assertEquals(4, set.records().size());

            Record r = set.records().get(0);
            switch (set.conceptAlias()) {
                case "samples":
                    assertTrue(r.has("urn:materialSampleID"));
                    assertTrue(r.has("urn:principalInvestigator"));
                    assertTrue(r.has("urn:decimalLatitude"));
                    assertTrue(r.has("urn:decimalLongitude"));
                    assertTrue(r.has("urn:timeCollected"));
                    assertTrue(r.has("urn:elevation"));
                    break;
                case "events":
                    assertTrue(r.has("urn:hasLegs"));
                    assertTrue(r.has("urn:collectionTimestamp"));
                    assertTrue(r.has("urn:ObservationDate"));
                    assertTrue(r.has("urn:locality"));
                    break;
                default:
                    fail("Should only contain \"samples\" and \"events\" RecordSets");
            }
        }
    }

    @Test
    public void should_throw_exception_with_duplicate_columns() {
        File csvFile = new File(classLoader.getResource("duplicateColumnDataset.csv").getFile());

        RecordMetadata rm = new RecordMetadata(GenericRecord.class);
        rm.add(CSVReader.SHEET_NAME_KEY, "sheet1");
        DataReader reader = new CSVReader(csvFile, getSingleEntityMapping(), rm);

        try {
            List<RecordSet> recordSets = reader.getRecordSets();
            fail();
        } catch (FimsRuntimeException e) {
            assertEquals(DataReaderCode.DUPLICATE_COLUMNS, e.getErrorCode());
        }
    }

    private Mapping getSingleEntityMapping() {
        Mapping mapping = new Mapping();

        Entity entity = new Entity();
        entity.setWorksheet("sheet1");
        entity.setConceptAlias("samples");
        mapping.addEntity(entity);

        Attribute a1 = new Attribute("materialSampleID", "urn:materialSampleID");
        Attribute a2 = new Attribute("principalInvestigator", "urn:principalInvestigator");
        Attribute a3 = new Attribute("locality", "urn:locality");
        Attribute a4 = new Attribute("phylum", "urn:phylum");
        Attribute a5 = new Attribute("Latitude", "urn:decimalLatitude");
        Attribute a6 = new Attribute("decimalLongitude", "urn:decimalLongitude");
        Attribute a7 = new Attribute("Observation_Date", "urn:ObservationDate");
        Attribute a8 = new Attribute("Time Collected", "urn:timeCollected");
        Attribute a9 = new Attribute("Collection TimeStamp", "urn:collectionTimestamp");
        Attribute a10 = new Attribute("Elevation", "urn:elevation");
        Attribute a11 = new Attribute("hasLegs", "urn:hasLegs");
        entity.addAttribute(a1);
        entity.addAttribute(a2);
        entity.addAttribute(a3);
        entity.addAttribute(a4);
        entity.addAttribute(a5);
        entity.addAttribute(a6);
        entity.addAttribute(a7);
        entity.addAttribute(a8);
        entity.addAttribute(a9);
        entity.addAttribute(a10);
        entity.addAttribute(a11);

        return mapping;
    }

    private Mapping getMultipleEntityMapping() {
        Mapping mapping = new Mapping();

        Entity entity1 = new Entity();
        entity1.setWorksheet("sheet1");
        entity1.setConceptAlias("samples");
        mapping.addEntity(entity1);

        Entity entity2 = new Entity();
        entity2.setWorksheet("sheet1");
        entity2.setConceptAlias("events");
        mapping.addEntity(entity2);

        Attribute a1 = new Attribute("materialSampleID", "urn:materialSampleID");
        Attribute a2 = new Attribute("principalInvestigator", "urn:principalInvestigator");
        Attribute a3 = new Attribute("Elevation", "urn:elevation");
        Attribute a4 = new Attribute("phylum", "urn:phylum");
        Attribute a5 = new Attribute("Latitude", "urn:decimalLatitude");
        Attribute a6 = new Attribute("decimalLongitude", "urn:decimalLongitude");
        Attribute a7 = new Attribute("Time Collected", "urn:timeCollected");
        entity1.addAttribute(a1);
        entity1.addAttribute(a2);
        entity1.addAttribute(a3);
        entity1.addAttribute(a4);
        entity1.addAttribute(a5);
        entity1.addAttribute(a6);
        entity1.addAttribute(a7);

        Attribute a8 = new Attribute("hasLegs", "urn:hasLegs");
        Attribute a9 = new Attribute("Collection TimeStamp", "urn:collectionTimestamp");
        Attribute a10 = new Attribute("Observation_Date", "urn:ObservationDate");
        Attribute a11 = new Attribute("locality", "urn:locality");
        entity2.addAttribute(a8);
        entity2.addAttribute(a9);
        entity2.addAttribute(a10);
        entity2.addAttribute(a11);

        return mapping;
    }
}
