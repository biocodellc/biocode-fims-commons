package biocode.fims.models.records;

import java.util.HashMap;
import java.util.Map;

/**
 * @author rjewing
 */
public class GenericRecord {
    private Map<String, String> properties;

    public GenericRecord(Map<String, String> properties) {
        this.properties = properties;
    }

    public GenericRecord() {
        this.properties = new HashMap<>();
    }

    public String get(String property) {
        return properties.getOrDefault(property, "");
    }

    public void set(String property, String value) {
        properties.put(property, value);
    }
}
