package biocode.fims.models.records;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author rjewing
 */
public class GenericRecord implements Record {
    private Map<String, String> properties;

    public GenericRecord(Map<String, String> properties) {
        this.properties = properties;
    }

    public GenericRecord() {
        this.properties = new HashMap<>();
    }

    @Override
    public String get(String property) {
        return properties.getOrDefault(property, "").trim();
    }

    @Override
    public boolean has(String property) {
        return properties.containsKey(property);
    }

    @Override
    public void set(String property, String value) {
        properties.put(property, value);
    }

    @Override
    public List<Record> all() {
        return Arrays.asList(new GenericRecord(), new GenericRecord());
    }

    @Override
    public void setMetadata(RecordMetadata recordMetadata) {}

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof GenericRecord)) return false;

        GenericRecord record = (GenericRecord) o;

        return properties.equals(record.properties);
    }

    @Override
    public int hashCode() {
        return properties.hashCode();
    }
}
