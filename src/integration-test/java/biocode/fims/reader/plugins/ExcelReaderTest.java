package biocode.fims.reader.plugins;

import biocode.fims.projectConfig.models.Attribute;
import biocode.fims.projectConfig.models.Entity;
import biocode.fims.fimsExceptions.FimsRuntimeException;
import biocode.fims.fimsExceptions.errorCodes.DataReaderCode;
import biocode.fims.models.records.Record;
import biocode.fims.models.records.RecordMetadata;
import biocode.fims.models.records.RecordSet;
import biocode.fims.projectConfig.ProjectConfig;
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
            new ExcelReader(new File("test.xls"), new ProjectConfig(), null);
            fail();
        } catch (IllegalArgumentException e) {
        }

    }

    @Test
    public void should_throw_exception_if_no_data() {
        File csvFile = new File(classLoader.getResource("noDataDataset.xlsx").getFile());

        DataReader reader = new ExcelReader(csvFile, getSingleEntityConfig(), new RecordMetadata(TabularDataReaderType.READER_TYPE, false));

        try {
            reader.getRecordSets();
            fail();
        } catch (FimsRuntimeException e) {
            assertEquals(DataReaderCode.NO_DATA, e.getErrorCode());
        }
    }

    @Test
    public void should_throw_exception_if_only_headers() {
        File csvFile = new File(classLoader.getResource("onlyHeadersDataset.xlsx").getFile());

        DataReader reader = new ExcelReader(csvFile, getSingleEntityConfig(), new RecordMetadata(TabularDataReaderType.READER_TYPE, false));

        try {
            reader.getRecordSets();
            fail();
        } catch (FimsRuntimeException e) {
            assertEquals(DataReaderCode.NO_DATA, e.getErrorCode());
        }
    }

    @Test
    public void should_return_all_records_for_single_entity_single_sheet_mapping() {
        File excelFile = new File(classLoader.getResource("singleSheetDataset.xlsx").getFile());

        DataReader reader = new ExcelReader(excelFile, getSingleEntityConfig(), new RecordMetadata(TabularDataReaderType.READER_TYPE, false));

        List<RecordSet> recordSets = reader.getRecordSets();

        assertEquals(1, recordSets.size());

        verifySingleSheetSingleEntityRecordSets(recordSets);
    }

    @Test
    public void should_return_all_records_for_single_entity_single_sheet_mapping_xls_file() {
        File excelFile = new File(classLoader.getResource("singleSheetDataset.xls").getFile());

        DataReader reader = new ExcelReader(excelFile, getSingleEntityConfig(), new RecordMetadata(TabularDataReaderType.READER_TYPE, false));

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

        DataReader reader = new ExcelReader(excelFile, getMultipleEntityConfig(), new RecordMetadata(TabularDataReaderType.READER_TYPE, false));

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

        DataReader reader = new ExcelReader(excelFile, getMultipleEntityMultiSheetConfig(), new RecordMetadata(TabularDataReaderType.READER_TYPE, false));

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

    private ProjectConfig getSingleEntityConfig() {
        ProjectConfig config = new ProjectConfig();

        Entity entity = new Entity("samples", "someURI");
        entity.setWorksheet("samples");
        config.addEntity(entity);

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

        return config;
    }

    private ProjectConfig getMultipleEntityConfig() {
        ProjectConfig config = new ProjectConfig();

        Entity entity1 = new Entity("samples", "someURI");
        entity1.setWorksheet("samples");
        config.addEntity(entity1);

        Entity entity2 = new Entity("tissues", "someURI");
        entity2.setWorksheet("samples");
        config.addEntity(entity2);

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

        return config;
    }

    private ProjectConfig getMultipleEntityMultiSheetConfig() {
        ProjectConfig config = getMultipleEntityConfig();

        Entity entity = new Entity("events", "someURI");
        entity.setWorksheet("events");
        config.addEntity(entity);

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

        return config;
    }
}
