package biocode.fims.reader;

import biocode.fims.fimsExceptions.FimsRuntimeException;
import biocode.fims.fimsExceptions.errorCodes.DataReaderCode;
import biocode.fims.fimsExceptions.errorCodes.FileCode;
import biocode.fims.models.records.RecordMetadata;
import biocode.fims.projectConfig.ProjectConfig;
import biocode.fims.reader.plugins.CSVReader;
import org.junit.Before;
import org.junit.Test;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

/**
 * @author rjewing
 */
public class DataReaderFactoryTest {

    private ClassLoader classLoader;

    @Before
    public void setUp() throws Exception {
        if (classLoader == null) {
            classLoader = getClass().getClassLoader();
        }
    }

    @Test
    public void should_throw_exception_if_file_not_found() {
        DataReaderFactory rf = new DataReaderFactory(new HashMap<>(), dataReadersForEntity);

        try {
            rf.getReader("fakeFile.txt", null, null);
            fail();
        } catch (FimsRuntimeException e) {
            assertEquals(FileCode.READ_ERROR, e.getErrorCode());
        }
    }

    @Test
    public void should_throw_exception_if_reader_not_found_for_file_extension() {
        DataReaderFactory rf = new DataReaderFactory(new HashMap<>(), dataReadersForEntity);

        try {
            rf.getReader(classLoader.getResource("testDataset.csv").getFile(), null, new RecordMetadata(TabularDataReaderType.READER_TYPE, false));
            fail();
        } catch (FimsRuntimeException e) {
            assertEquals(DataReaderCode.NOT_FOUND, e.getErrorCode());
        }
    }

    @Test
    public void should_return_csv_reader() {
        Map<DataReader.DataReaderType, List<DataReader>> dataReaders = new HashMap<>();
        dataReaders.put(TabularDataReaderType.READER_TYPE, Collections.singletonList(new CSVReader()));

        DataReaderFactory rf = new DataReaderFactory(dataReaders, dataReadersForEntity);

        RecordMetadata rm = new RecordMetadata(TabularDataReaderType.READER_TYPE, false);
        rm.add("sheetName", "test");

        DataReader reader = rf.getReader(classLoader.getResource("testDataset.csv").getFile(), new ProjectConfig(), rm);

        assertEquals(CSVReader.class, reader.getClass());
    }

}