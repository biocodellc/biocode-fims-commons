package biocode.fims.models.records;

import org.apache.commons.collections.map.HashedMap;
import org.springframework.util.Assert;

import java.util.HashMap;
import java.util.Map;

/**
 * Class to store generic properties used when constructing {@link Record} instances.
 * <p>
 * Record classes each require their own unique metadata, as well as {@link biocode.fims.reader.plugins.DataReader}
 * which need metadata to be able to create the Records from a data file. This class is a container for this information
 *
 * @author rjewing
 */
public class RecordMetadata {
    private final Class<? extends Record> recordType;
    private final Map<String, Object> metadata;

    public RecordMetadata(Class<? extends Record> recordType) {
        Assert.notNull(recordType);
        this.recordType = recordType;
        this.metadata = new HashMap();
    }

    public RecordMetadata(Class<? extends Record> recordType, Map<String, Object> metadata) {
        Assert.notNull(recordType);
        Assert.notNull(metadata);
        this.recordType = recordType;
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

    public Class<? extends Record> type() {
        return recordType;
    }
}
