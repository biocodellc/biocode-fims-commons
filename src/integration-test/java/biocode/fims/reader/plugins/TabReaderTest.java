package biocode.fims.reader.plugins;

import biocode.fims.records.RecordMetadata;
import biocode.fims.projectConfig.ProjectConfig;
import biocode.fims.reader.DataReader;
import biocode.fims.reader.TabularDataReaderType;
import org.junit.Test;

import java.io.File;

import static org.junit.Assert.*;

/**
 * @author rjewing
 */
public class TabReaderTest extends DelimitedTextReaderTest {

    @Test
    public void test_not_null_assertions() {
        try {
            new TabReader(null, null, null);
            fail();
        } catch (IllegalArgumentException e) {
        }

        try {
            new TabReader(new File("test.txt"), null, null);
            fail();
        } catch (IllegalArgumentException e) {
        }

        try {
            new TabReader(new File("test.txt"), new ProjectConfig(), null);
            fail();
        } catch (IllegalArgumentException e) {
        }

    }

    @Test
    public void should_return_all_records_for_single_entity_mappping() {
        File tsvFile = new File(classLoader.getResource("testDataset.txt").getFile());

        RecordMetadata rm = new RecordMetadata(TabularDataReaderType.READER_TYPE, false);
        rm.add(TabReader.SHEET_NAME_KEY, "sheet1");
        DataReader reader = new TabReader(tsvFile, getSingleEntityConfig(), rm);

        verifySingleEntityMapping(reader);
    }

    @Test
    public void should_return_all_records_for_multiple_entity_mappping() {
        File tsvFile = new File(classLoader.getResource("testDataset.txt").getFile());

        RecordMetadata rm = new RecordMetadata(TabularDataReaderType.READER_TYPE, false);
        rm.add(TabReader.SHEET_NAME_KEY, "sheet1");
        DataReader reader = new TabReader(tsvFile, getMultipleEntityConfig(), rm);

        verifyMultiEntityMapping(reader);
    }

}
