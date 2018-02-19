package biocode.fims.models.records;

import biocode.fims.reader.DataReader;
import org.springframework.util.Assert;

import java.util.HashMap;
import java.util.Map;

/**
 * Class to store generic properties used when constructing {@link Record} instances.
 * <p>
 * Record classes each require their own unique metadata, as well as {@link DataReader}
 * which need metadata to be able to create the Records from a data file. This class is a container for this information
 *
 * @author rjewing
 */
public class RecordMetadata {
    private DataReader.DataReaderType readerType;
    private final Map<String, Object> metadata;

    public RecordMetadata(DataReader.DataReaderType readerType) {
        Assert.notNull(readerType);
        this.readerType = readerType;
        this.metadata = new HashMap();
    }

    public RecordMetadata(DataReader.DataReaderType readerType, Map<String, Object> metadata) {
        Assert.notNull(readerType);
        Assert.notNull(metadata);
        this.readerType = readerType;
        this.metadata = metadata;
    }

    public void add(String key, Object val) {
        metadata.put(key, val);
    }

    public Object get(String key) {
        return metadata.get(key);
    }

    public Object remove(String key) {
        return metadata.remove(key);
    }

    public boolean has(String key) {
        return metadata.keySet().contains(key);
    }

    public Map<String, Object> metadata() { return metadata; }

    public DataReader.DataReaderType readerType() {
        return readerType;
    }
}
