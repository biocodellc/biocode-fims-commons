package biocode.fims.records;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * @author rjewing
 */
public class GenericRecord implements Record {
    protected Map<String, String> properties;
    private String rootIdentifier;
    private int projectId;
    private String expeditionCode;
    protected boolean persist = true;
    private boolean hasError = false;

    public GenericRecord(Map<String, String> properties, String rootIdentifier, int projectId, String expeditionCode, boolean shouldPersist) {
        this.properties = properties;
        this.rootIdentifier = rootIdentifier;
        this.projectId = projectId;
        this.expeditionCode = expeditionCode;
        this.persist = shouldPersist;
        removeFieldProperties();
    }

    public GenericRecord(Map<String, String> properties) {
        this.properties = properties;
        removeFieldProperties();
    }

    public GenericRecord() {
        this.properties = new HashMap<>();
    }

    @Override
    public int projectId() {
        return projectId;
    }

    @Override
    public void setProjectId(int projectId) {
        if (this.projectId == 0) this.projectId = projectId;
        else throw new IllegalStateException("projectId has already been set");
    }

    @Override
    public String expeditionCode() {
        return expeditionCode;
    }

    @Override
    public void setExpeditionCode(String expeditionCode) {
        this.expeditionCode = expeditionCode;
    }

    @Override
    public String rootIdentifier() {
        return rootIdentifier;
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
    public void setError() {
        hasError = true;
    }

    @Override
    public void setMetadata(RecordMetadata recordMetadata) {
    }

    @Override
    public boolean persist() {
        return !hasError && persist;
    }

    private void removeFieldProperties() {
        if (properties.containsKey(Record.EXPEDITION_CODE)) {
            setExpeditionCode(properties.remove(Record.EXPEDITION_CODE));
        }
        if (properties.containsKey(Record.PROJECT_ID)) {
            setProjectId(Integer.parseInt(properties.remove(Record.PROJECT_ID)));
        }
        if (properties.containsKey(Record.ROOT_IDENTIFIER)) {
            properties.remove(Record.ROOT_IDENTIFIER);
        }
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
