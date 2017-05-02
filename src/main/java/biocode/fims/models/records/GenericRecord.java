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
        return properties.getOrDefault(property, "");
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
}
