package biocode.fims.reader.plugins;

import biocode.fims.digester.Mapping;
import biocode.fims.fimsExceptions.FimsRuntimeException;
import biocode.fims.fimsExceptions.errorCodes.DataReaderCode;
import biocode.fims.models.records.RecordMetadata;
import biocode.fims.models.records.RecordSet;
import biocode.fims.reader.DataReader;
import biocode.fims.reader.TabularDataReaderType;
import org.junit.Test;

import java.io.File;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

/**
 * @author rjewing
 */
public class CSVReaderTest extends DelimitedTextReaderTest {

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

        RecordMetadata rm = new RecordMetadata(TabularDataReaderType.READER_TYPE);
        rm.add(DelimitedTextReader.SHEET_NAME_KEY, "sheet1");
        DataReader reader = new CSVReader(csvFile, getSingleEntityMapping(), rm);

        verifySingleEntityMapping(reader);
    }

    @Test
    public void should_return_all_records_for_multiple_entity_mappping() {
        File csvFile = new File(classLoader.getResource("testDataset.csv").getFile());

        RecordMetadata rm = new RecordMetadata(TabularDataReaderType.READER_TYPE);
        rm.add(DelimitedTextReader.SHEET_NAME_KEY, "sheet1");
        DataReader reader = new CSVReader(csvFile, getMultipleEntityMapping(), rm);

        verifyMultiEntityMapping(reader);
    }

    @Test
    public void should_throw_exception_with_duplicate_columns() {
        File csvFile = new File(classLoader.getResource("duplicateColumnDataset.csv").getFile());

        RecordMetadata rm = new RecordMetadata(TabularDataReaderType.READER_TYPE);
        rm.add(DelimitedTextReader.SHEET_NAME_KEY, "sheet1");
        DataReader reader = new CSVReader(csvFile, getSingleEntityMapping(), rm);

        try {
            reader.getRecordSets();
            fail();
        } catch (FimsRuntimeException e) {
            assertEquals(DataReaderCode.DUPLICATE_COLUMNS, e.getErrorCode());
        }
    }

    @Test
    public void should_throw_exception_if_only_headers() {
        File csvFile = new File(classLoader.getResource("onlyHeadersDataset.csv").getFile());

        RecordMetadata rm = new RecordMetadata(TabularDataReaderType.READER_TYPE);
        rm.add(DelimitedTextReader.SHEET_NAME_KEY, "sheet1");
        DataReader reader = new CSVReader(csvFile, getSingleEntityMapping(), rm);

        try {
            reader.getRecordSets();
            fail();
        } catch (FimsRuntimeException e) {
            assertEquals(DataReaderCode.NO_DATA, e.getErrorCode());
        }
    }

    @Test
    public void should_throw_exception_if_no_data() {
        File csvFile = new File(classLoader.getResource("noDataDataset.csv").getFile());

        RecordMetadata rm = new RecordMetadata(TabularDataReaderType.READER_TYPE);
        rm.add(DelimitedTextReader.SHEET_NAME_KEY, "sheet1");
        DataReader reader = new CSVReader(csvFile, getSingleEntityMapping(), rm);

        try {
            reader.getRecordSets();
            fail();
        } catch (FimsRuntimeException e) {
            assertEquals(DataReaderCode.NO_DATA, e.getErrorCode());
        }
    }
}
