package biocode.fims.reader.plugins;

import biocode.fims.digester.Mapping;
import biocode.fims.models.records.RecordMetadata;
import biocode.fims.models.records.RecordSet;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author rjewing
 */
public class TestDataReader implements DataReader {
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
    public DataReader newInstance(File file, Mapping mapping, RecordMetadata recordMetadata) {
        this.filename = file.getName();
        return this;
    }
}
