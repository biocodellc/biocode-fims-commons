package biocode.fims.reader.plugins;

import biocode.fims.digester.Attribute;
import biocode.fims.digester.Entity;
import biocode.fims.digester.Mapping;
import biocode.fims.models.records.Record;
import biocode.fims.models.records.RecordMetadata;
import biocode.fims.models.records.RecordSet;
import biocode.fims.reader.DataReader;
import biocode.fims.reader.TabularDataReaderType;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.util.List;
import java.util.Optional;

import static org.junit.Assert.*;

/**
 * @author rjewing
 */
public class ExcelReaderTest {

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
            new ExcelReader(null, null, null);
            fail();
        } catch (IllegalArgumentException e) {
        }

        try {
            new ExcelReader(new File("test.xls"), null, null);
            fail();
        } catch (IllegalArgumentException e) {
        }

        try {
            new ExcelReader(new File("test.xls"), new Mapping(), null);
            fail();
        } catch (IllegalArgumentException e) {
        }

    }


    @Test
    public void should_return_all_records_for_single_entity_single_sheet_mapping() {
        File excelFile = new File(classLoader.getResource("singleSheetDataset.xlsx").getFile());

        DataReader reader = new ExcelReader(excelFile, getSingleEntityMapping(), new RecordMetadata(TabularDataReaderType.READER_TYPE));

        List<RecordSet> recordSets = reader.getRecordSets();

        assertEquals(1, recordSets.size());

        verifySingleSheetSingleEntityRecordSets(recordSets);
    }

    @Test
    public void should_return_all_records_for_single_entity_single_sheet_mapping_xls_file() {
        File excelFile = new File(classLoader.getResource("singleSheetDataset.xls").getFile());

        DataReader reader = new ExcelReader(excelFile, getSingleEntityMapping(), new RecordMetadata(TabularDataReaderType.READER_TYPE));

        List<RecordSet> recordSets = reader.getRecordSets();

        assertEquals(1, recordSets.size());

        verifySingleSheetSingleEntityRecordSets(recordSets);
    }

    private void verifySingleSheetSingleEntityRecordSets(List<RecordSet> recordSets) {
        for (RecordSet set: recordSets) {
            assertEquals("samples", set.conceptAlias());
            assertEquals(3, set.records().size());

            Record r = set.records().get(0);

            assertTrue(r.has("urn:sampleID"));
            assertTrue(r.has("urn:principalInvestigator"));
            assertTrue(r.has("urn:family"));
            assertTrue(r.has("urn:order"));
            assertTrue(r.has("urn:phylum"));
            assertTrue(r.has("urn:genus"));
            assertTrue(r.has("urn:species"));
            assertTrue(r.has("urn:preservative"));
            assertTrue(r.has("urn:container"));
            assertTrue(r.has("urn:96_well_num"));
        }
    }

    @Test
    public void should_return_all_records_for_multiple_entity_single_sheet_mappping() {
        File excelFile = new File(classLoader.getResource("singleSheetDataset.xlsx").getFile());

        DataReader reader = new ExcelReader(excelFile, getMultipleEntityMapping(), new RecordMetadata(TabularDataReaderType.READER_TYPE));

        List<RecordSet> recordSets = reader.getRecordSets();

        assertEquals(2, recordSets.size());

        for (RecordSet set: recordSets) {
            assertEquals(3, set.records().size());

            Record r = set.records().get(0);
            switch (set.conceptAlias()) {
                case "samples":
                    assertTrue(r.has("urn:sampleID"));
                    assertTrue(r.has("urn:principalInvestigator"));
                    assertTrue(r.has("urn:family"));
                    assertTrue(r.has("urn:order"));
                    assertTrue(r.has("urn:phylum"));
                    assertTrue(r.has("urn:genus"));
                    assertTrue(r.has("urn:species"));
                    break;
                case "tissues":
                    assertTrue(r.has("urn:preservative"));
                    assertTrue(r.has("urn:container"));
                    assertTrue(r.has("urn:96_well_num"));
                    break;
                default:
                    fail("Should only contain \"samples\" and \"tissues\" RecordSets");
            }
        }
    }

    @Test
    public void should_return_all_records_for_multiple_entity_multi_sheet_mappping() {
        File excelFile = new File(classLoader.getResource("multiSheetDataset.xlsx").getFile());

        DataReader reader = new ExcelReader(excelFile, getMultipleEntityMultiSheetMapping(), new RecordMetadata(TabularDataReaderType.READER_TYPE));

        List<RecordSet> recordSets = reader.getRecordSets();

        assertEquals(3, recordSets.size());

        Optional<RecordSet> eventsSet = recordSets.stream().filter(r -> r.conceptAlias().equals("events")).findFirst();

        assertTrue(eventsSet.isPresent());
        assertEquals(1, eventsSet.get().records().size());

        Record eventsRecord = eventsSet.get().records().get(0);
        assertTrue(eventsRecord.has("urn:location"));
        assertTrue(eventsRecord.has("urn:country"));
        assertTrue(eventsRecord.has("urn:island"));
        assertTrue(eventsRecord.has("urn:latitude"));
        assertTrue(eventsRecord.has("urn:longitude"));

        Optional<RecordSet> samplesSet = recordSets.stream().filter(r -> r.conceptAlias().equals("samples")).findFirst();

        assertTrue(samplesSet.isPresent());
        assertEquals(3, samplesSet.get().records().size());

        Record samplesRecord = samplesSet.get().records().get(0);
        assertTrue(samplesRecord.has("urn:sampleID"));
        assertTrue(samplesRecord.has("urn:principalInvestigator"));
        assertTrue(samplesRecord.has("urn:family"));
        assertTrue(samplesRecord.has("urn:order"));
        assertTrue(samplesRecord.has("urn:phylum"));
        assertTrue(samplesRecord.has("urn:genus"));
        assertTrue(samplesRecord.has("urn:species"));

        Optional<RecordSet> tissuesSet = recordSets.stream().filter(r -> r.conceptAlias().equals("tissues")).findFirst();

        assertTrue(tissuesSet.isPresent());
        assertEquals(3, tissuesSet.get().records().size());

        Record tissuesRecord = tissuesSet.get().records().get(0);
        assertTrue(tissuesRecord.has("urn:preservative"));
        assertTrue(tissuesRecord.has("urn:container"));
        assertTrue(tissuesRecord.has("urn:96_well_num"));
    }

    private Mapping getSingleEntityMapping() {
        Mapping mapping = new Mapping();

        Entity entity = new Entity();
        entity.setWorksheet("samples");
        entity.setConceptAlias("samples");
        mapping.addEntity(entity);

        Attribute a1 = new Attribute("sampleID", "urn:sampleID");
        Attribute a2 = new Attribute("principalInvestigator", "urn:principalInvestigator");
        Attribute a3 = new Attribute("family", "urn:family");
        Attribute a4 = new Attribute("order", "urn:order");
        Attribute a5 = new Attribute("phylum", "urn:phylum");
        Attribute a6 = new Attribute("genus", "urn:genus");
        Attribute a7 = new Attribute("species", "urn:species");
        Attribute a8 = new Attribute("preservative", "urn:preservative");
        Attribute a9 = new Attribute("container", "urn:container");
        Attribute a10 = new Attribute("96 Well Number", "urn:96_well_num");
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

        return mapping;
    }

    private Mapping getMultipleEntityMapping() {
        Mapping mapping = new Mapping();

        Entity entity1 = new Entity();
        entity1.setWorksheet("samples");
        entity1.setConceptAlias("samples");
        mapping.addEntity(entity1);

        Entity entity2 = new Entity();
        entity2.setWorksheet("samples");
        entity2.setConceptAlias("tissues");
        mapping.addEntity(entity2);

        Attribute a1 = new Attribute("sampleID", "urn:sampleID");
        Attribute a2 = new Attribute("principalInvestigator", "urn:principalInvestigator");
        Attribute a3 = new Attribute("family", "urn:family");
        Attribute a4 = new Attribute("order", "urn:order");
        Attribute a5 = new Attribute("phylum", "urn:phylum");
        Attribute a6 = new Attribute("genus", "urn:genus");
        Attribute a7 = new Attribute("species", "urn:species");
        entity1.addAttribute(a1);
        entity1.addAttribute(a2);
        entity1.addAttribute(a3);
        entity1.addAttribute(a4);
        entity1.addAttribute(a5);
        entity1.addAttribute(a6);
        entity1.addAttribute(a7);

        Attribute a8 = new Attribute("preservative", "urn:preservative");
        Attribute a9 = new Attribute("container", "urn:container");
        Attribute a10 = new Attribute("96 Well Number", "urn:96_well_num");
        entity2.addAttribute(a8);
        entity2.addAttribute(a9);
        entity2.addAttribute(a10);

        return mapping;
    }

    private Mapping getMultipleEntityMultiSheetMapping() {
        Mapping mapping = getMultipleEntityMapping();

        Entity entity = new Entity();
        entity.setConceptAlias("events");
        entity.setWorksheet("events");
        mapping.addEntity(entity);

        Attribute a1 = new Attribute("location", "urn:location");
        Attribute a2 = new Attribute("country", "urn:country");
        Attribute a3 = new Attribute("island", "urn:island");
        Attribute a4 = new Attribute("latitude", "urn:latitude");
        Attribute a5 = new Attribute("longitude", "urn:longitude");
        entity.addAttribute(a1);
        entity.addAttribute(a2);
        entity.addAttribute(a3);
        entity.addAttribute(a4);
        entity.addAttribute(a5);

        return mapping;
    }
}
