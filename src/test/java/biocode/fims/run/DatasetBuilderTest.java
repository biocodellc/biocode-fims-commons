package biocode.fims.run;

import biocode.fims.projectConfig.models.Attribute;
import biocode.fims.projectConfig.models.Entity;
import biocode.fims.fimsExceptions.FimsRuntimeException;
import biocode.fims.fimsExceptions.errorCodes.FileCode;
import biocode.fims.fimsExceptions.errorCodes.ValidationCode;
import biocode.fims.models.records.GenericRecord;
import biocode.fims.models.records.Record;
import biocode.fims.models.records.RecordMetadata;
import biocode.fims.models.records.RecordSet;
import biocode.fims.projectConfig.ProjectConfig;
import biocode.fims.reader.DataConverter;
import biocode.fims.reader.DataConverterFactory;
import biocode.fims.reader.DataReaderFactory;
import biocode.fims.reader.plugins.CSVReader;
import biocode.fims.reader.DataReader;
import biocode.fims.reader.plugins.TestDataReader;
import biocode.fims.repositories.TestRecordRepository;
import org.junit.Test;

import java.io.File;
import java.util.*;

import static junit.framework.Assert.assertEquals;
import static junit.framework.TestCase.fail;
import static org.junit.Assert.assertTrue;

/**
 * @author rjewing
 */
public class DatasetBuilderTest {
    private static final int PROJECT_ID = 1;
    private static final String EXPEDITION_CODE = "demo";

    @Test
    public void should_throw_exception_if_empty_dataset() {
        DatasetBuilder builder = new DatasetBuilder(null, null, null, config(), PROJECT_ID, EXPEDITION_CODE);

        try {
            builder.build();
            fail();
        } catch (FimsRuntimeException e) {
            assertEquals(ValidationCode.EMPTY_DATASET, e.getErrorCode());
        }
    }

    @Test
    public void should_throw_exception_non_excel_file_passed_to_workbook() {
        DatasetBuilder builder = new DatasetBuilder(null, null, null, config(), PROJECT_ID, EXPEDITION_CODE);

        try {
            builder.addWorkbook("dataSource.txt");
            fail();
        } catch (FimsRuntimeException e) {
            assertEquals(FileCode.INVALID_FILE, e.getErrorCode());
        }
    }

    @Test
    public void should_return_record_sets_for_workbook_file() {
        TestDataReader reader = new TestDataReader();
        reader.addRecordSet("dataSource.xls", eventRecordSet(false));

        DatasetBuilder builder = new DatasetBuilder(dataReaderFactory(reader), dataConverterFactory(null), new TestRecordRepository(), config(), PROJECT_ID, EXPEDITION_CODE);
        builder.addWorkbook("dataSource.xls");

        Dataset dataset = builder.build();

        assertEquals(1, dataset.size());
        assertEquals(1, dataset.get(0).records().size());
    }

    @Test
    public void should_return_record_sets_for_non_workbook_file() {
        TestDataReader reader = new TestDataReader();
        reader.addRecordSet("dataSource.xls", eventRecordSet(false));
        reader.addRecordSet("samples.csv", sampleRecordSet());

        DatasetBuilder builder = new DatasetBuilder(dataReaderFactory(reader), dataConverterFactory(null), new TestRecordRepository(), config(), PROJECT_ID, EXPEDITION_CODE);
        builder.addWorkbook("dataSource.xls");

        RecordMetadata samplesMetadata = new RecordMetadata(TestDataReader.READER_TYPE, false);
        samplesMetadata.add(CSVReader.SHEET_NAME_KEY, "samples");
        builder.addDatasource("samples.csv", samplesMetadata);

        Dataset dataset = builder.build();

        assertEquals(2, dataset.size());

        Optional<RecordSet> eventsSet = dataset.stream().filter(r -> r.conceptAlias().equals("event")).findFirst();

        assertTrue(eventsSet.isPresent());
        assertEquals(1, eventsSet.get().records().size());

        Optional<RecordSet> samplesSet = dataset.stream().filter(r -> r.conceptAlias().equals("sample")).findFirst();

        assertTrue(samplesSet.isPresent());
        assertEquals(4, samplesSet.get().records().size());
    }

    @Test
    public void should_fetch_stored_parent_records_for_child_entites() {
        TestDataReader reader = new TestDataReader();
        reader.addRecordSet("samples.csv", sampleRecordSet());

        TestRecordRepository repository = new TestRecordRepository();
        repository.addRecord(PROJECT_ID, EXPEDITION_CODE, "event", eventRecord1());
        repository.addRecord(PROJECT_ID, EXPEDITION_CODE, "event", eventRecord2());

        RecordMetadata samplesMetadata = new RecordMetadata(TestDataReader.READER_TYPE, false);
        samplesMetadata.add(CSVReader.SHEET_NAME_KEY, "samples");

        Dataset dataset = new DatasetBuilder(
                dataReaderFactory(reader),
                dataConverterFactory(null),
                repository,
                config(),
                PROJECT_ID,
                EXPEDITION_CODE
        )
                .addDatasource("samples.csv", samplesMetadata)
                .build();

        assertEquals(2, dataset.size());

        Optional<RecordSet> eventsSet = dataset.stream().filter(r -> r.conceptAlias().equals("event")).findFirst();

        assertTrue(eventsSet.isPresent());
        assertEquals(2, eventsSet.get().records().size());

        Optional<RecordSet> samplesSet = dataset.stream().filter(r -> r.conceptAlias().equals("sample")).findFirst();

        assertTrue(samplesSet.isPresent());
        assertEquals(4, samplesSet.get().records().size());
        assertEquals(eventsSet.get(), samplesSet.get().parent());
    }

    @Test
    public void should_fetch_stored_parent_records_for_child_entities_with_parent_entity_upload_when_updating_dataset() {
        TestDataReader reader = new TestDataReader();
        reader.addRecordSet("workbook.xlsx", sampleRecordSet());
        reader.addRecordSet("workbook.xlsx", eventRecordSet(false));

        TestRecordRepository repository = new TestRecordRepository();
        repository.addRecord(PROJECT_ID, EXPEDITION_CODE, "event", eventRecord2());

        Dataset dataset = new DatasetBuilder(
                dataReaderFactory(reader),
                dataConverterFactory(null),
                repository,
                config(),
                PROJECT_ID,
                EXPEDITION_CODE
        )
                .addWorkbook("workbook.xlsx")
                .build();

        assertEquals(2, dataset.size());

        Optional<RecordSet> eventsSet = dataset.stream().filter(r -> r.conceptAlias().equals("event")).findFirst();

        assertTrue(eventsSet.isPresent());
        assertEquals(2, eventsSet.get().records().size());

        Optional<RecordSet> samplesSet = dataset.stream().filter(r -> r.conceptAlias().equals("sample")).findFirst();

        assertTrue(samplesSet.isPresent());
        assertEquals(4, samplesSet.get().records().size());
        assertEquals(eventsSet.get(), samplesSet.get().parent());
    }

    @Test
    public void should_not_fetch_stored_parent_records_for_child_entities_with_parent_entity_upload_containing_all_parent_entities_when_updating_dataset() {
        TestDataReader reader = new TestDataReader();
        reader.addRecordSet("workbook.xlsx", sampleRecordSet());

        Record e1 = eventRecord1();
        Record e2 = eventRecord2();
        RecordSet recordSet = new RecordSet(eventsEntity(), Arrays.asList(e1, e2), false);
        reader.addRecordSet("workbook.xlsx", recordSet);

        TestRecordRepository repository = new TestRecordRepository();
        repository.addRecord(PROJECT_ID, EXPEDITION_CODE, "event", eventRecord2());

        Dataset dataset = new DatasetBuilder(
                dataReaderFactory(reader),
                dataConverterFactory(null),
                repository,
                config(),
                PROJECT_ID,
                EXPEDITION_CODE
        )
                .addWorkbook("workbook.xlsx")
                .build();

        assertEquals(2, dataset.size());

        Optional<RecordSet> eventsSet = dataset.stream().filter(r -> r.conceptAlias().equals("event")).findFirst();

        assertTrue(eventsSet.isPresent());
        assertEquals(recordSet, eventsSet.get());

        Optional<RecordSet> samplesSet = dataset.stream().filter(r -> r.conceptAlias().equals("sample")).findFirst();

        assertTrue(samplesSet.isPresent());
        assertEquals(4, samplesSet.get().records().size());
        assertEquals(eventsSet.get(), samplesSet.get().parent());
    }

    @Test
    public void should_not_fetch_stored_parent_records_for_child_entities_when_reloading_dataset() {
        TestDataReader reader = new TestDataReader();
        reader.addRecordSet("workbook.xlsx", sampleRecordSet());
        reader.addRecordSet("workbook.xlsx", eventRecordSet(true));

        TestRecordRepository repository = new TestRecordRepository();
        repository.addRecord(PROJECT_ID, EXPEDITION_CODE, "event", eventRecord2());

        Dataset dataset = new DatasetBuilder(
                dataReaderFactory(reader),
                dataConverterFactory(null),
                repository,
                config(),
                PROJECT_ID,
                EXPEDITION_CODE
        )
                .addWorkbook("workbook.xlsx")
                .reloadWorkbooks(true)
                .build();

        assertEquals(2, dataset.size());

        Optional<RecordSet> eventsSet = dataset.stream().filter(r -> r.conceptAlias().equals("event")).findFirst();

        assertTrue(eventsSet.isPresent());
        assertEquals(1, eventsSet.get().records().size());

        Optional<RecordSet> samplesSet = dataset.stream().filter(r -> r.conceptAlias().equals("sample")).findFirst();

        assertTrue(samplesSet.isPresent());
        assertEquals(4, samplesSet.get().records().size());
        assertEquals(eventsSet.get(), samplesSet.get().parent());
    }


    private RecordSet eventRecordSet(boolean reload) {
        return new RecordSet(eventsEntity(), Collections.singletonList(eventRecord1()), reload);
    }

    private Record eventRecord1() {
        Record r = new GenericRecord();

        r.set("urn:eventID", "1");
        r.set("urn:location", "Channel");
        r.set("urn:country", "Belize");
        r.set("urn:island", "Twin Cays");
        r.set("urn:latitude", "16.83046");
        r.set("urn:longitude", "-88.10460");
        return r;
    }

    private Record eventRecord2() {
        Record r = new GenericRecord();

        r.set("urn:eventID", "2");
        r.set("urn:location", "Channel");
        r.set("urn:country", "Belize");
        r.set("urn:island", "Twin Cays");
        r.set("urn:latitude", "16.83046");
        r.set("urn:longitude", "-88.10460");

        return r;
    }

    private RecordSet sampleRecordSet() {
        List<Record> records = new ArrayList<>();

        Record r = new GenericRecord();
        r.set("urn:sampleID", "1");
        r.set("urn:pi", "John Doe");
        r.set("urn:genus", "Aiptasia");
        r.set("urn:species", "pulchella");
        records.add(r);

        Record r2 = new GenericRecord();
        r2.set("urn:sampleID", "2");
        r2.set("urn:pi", "John Doe");
        r2.set("urn:genus", "Aiptasia");
        r2.set("urn:species", "pulchella");
        records.add(r2);

        Record r3 = new GenericRecord();
        r3.set("urn:sampleID", "3");
        r3.set("urn:pi", "John Doe");
        r3.set("urn:genus", "Aiptasia");
        r3.set("urn:species", "pulchella");
        records.add(r3);

        Record r4 = new GenericRecord();
        r4.set("urn:sampleID", "4");
        r4.set("urn:pi", "John Doe");
        r4.set("urn:genus", "Aiptasia");
        r4.set("urn:species", "pulchella");
        records.add(r4);

        return new RecordSet(samplesEntity(), records, false);
    }

    private ProjectConfig config() {
        ProjectConfig config = new ProjectConfig();

        config.addEntity(eventsEntity());
        config.addEntity(samplesEntity());

        return config;
    }

    private Entity samplesEntity() {
        Entity entity = new Entity("sample", "someURI");
        entity.setWorksheet("samples");
        entity.setParentEntity("event");

        Attribute a1 = new Attribute("sampleID", "urn:sampleID");
        Attribute a2 = new Attribute("pi", "urn:pi");
        Attribute a3 = new Attribute("genus", "urn:genus");
        Attribute a4 = new Attribute("species", "urn:species");
        entity.addAttribute(a1);
        entity.addAttribute(a2);
        entity.addAttribute(a3);
        entity.addAttribute(a4);

        return entity;
    }

    private Entity eventsEntity() {
        Entity entity = new Entity("event", "someURI");
        entity.setWorksheet("events");
        entity.setUniqueKey("eventID");

        Attribute a1 = new Attribute("location", "urn:location");
        Attribute a2 = new Attribute("country", "urn:country");
        Attribute a3 = new Attribute("island", "urn:island");
        Attribute a4 = new Attribute("latitude", "urn:latitude");
        Attribute a5 = new Attribute("longitude", "urn:longitude");
        Attribute a6 = new Attribute("eventID", "urn:eventID");
        entity.addAttribute(a1);
        entity.addAttribute(a2);
        entity.addAttribute(a3);
        entity.addAttribute(a4);
        entity.addAttribute(a5);
        entity.addAttribute(a6);

        return entity;
    }

    private DataReaderFactory dataReaderFactory(DataReader reader) {
        return new DataReaderFactory(null) {
            @Override
            public DataReader getReader(String filepath, ProjectConfig projectConfig, RecordMetadata recordMetadata) {
                return reader.newInstance(new File(filepath), projectConfig, recordMetadata);
            }
        };
    }

    private DataConverterFactory dataConverterFactory(DataConverter converter) {
        return new DataConverterFactory(null) {
            @Override
            public DataConverter getConverter(String entityType, ProjectConfig projectConfig) {
                return converter != null ? converter : new DataConverter() {
                    @Override
                    public RecordSet convertRecordSet(RecordSet recordSet, int projectId, String expeditionCode) {
                        return recordSet;
                    }

                    @Override
                    public DataConverter newInstance(ProjectConfig projectConfig) {
                        return this;
                    }
                };
            }
        };
    }

}