package biocode.fims.reader.plugins;

import biocode.fims.models.records.RecordMetadata;
import biocode.fims.models.records.RecordSet;
import biocode.fims.projectConfig.ProjectConfig;
import biocode.fims.reader.DataReader;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author rjewing
 */
public class TestDataReader implements DataReader {
    public static final DataReaderType READER_TYPE = new DataReaderType("TEST");

    private Map<String, List<RecordSet>> recordSets;
    private String filename;

    public TestDataReader() {
        this.recordSets = new HashMap<>();
    }

    public void addRecordSet(String filename, RecordSet recordSet) {
        recordSets.computeIfAbsent(filename, k -> new ArrayList<>()).add(recordSet);
    }

    @Override
    public List<RecordSet> getRecordSets() {
        return recordSets.computeIfAbsent(this.filename, k -> new ArrayList<>());
    }

    @Override
    public boolean handlesExtension(String ext) {
        return true;
    }

    @Override
    public DataReader newInstance(File file, ProjectConfig projectConfig, RecordMetadata recordMetadata) {
        this.filename = file.getName();
        return this;
    }

    @Override
    public DataReaderType readerType() {
        return READER_TYPE;
    }
}
