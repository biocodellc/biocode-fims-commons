package biocode.fims.models.records;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * @author rjewing
 */
public class GenericRecord implements Record {
    protected Map<String, String> properties;
    private boolean persist = true;

    public GenericRecord(Map<String, String> properties, boolean shouldPersist) {
        this.properties = properties;
        this.persist = shouldPersist;
    }

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
    public Map<String, String> properties() {
        return Collections.unmodifiableMap(properties);
    }

    @Override
    public void set(String property, String value) {
        properties.put(property, value);
        persist = true;
    }

    @Override
    public void setMetadata(RecordMetadata recordMetadata) {}

    @Override
    public boolean persist() {
        return persist;
    }

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
