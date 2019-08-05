package biocode.fims.records;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * @author rjewing
 */
public class GenericRecord implements Record {
    protected Map<String, Object> properties;
    private String rootIdentifier;
    private int projectId;
    private String expeditionCode;
    protected boolean persist = true;
    private boolean hasError = false;

    public GenericRecord(Map<String, Object> properties, String rootIdentifier, int projectId, String expeditionCode, boolean shouldPersist) {
        this.properties = properties;
        this.rootIdentifier = rootIdentifier;
        this.projectId = projectId;
        this.expeditionCode = expeditionCode;
        this.persist = shouldPersist;
        removeFieldProperties();
    }

    public GenericRecord(Map<String, Object> properties) {
        this.properties = new HashMap<>(properties);
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
    public void setRootIdentifier(String rootIdentifier) {
        if (this.rootIdentifier == null) this.rootIdentifier = rootIdentifier;
        else throw new IllegalStateException("rootIdentifier has already been set");
    }

    @Override
    public String get(String property) {
        return String.valueOf(properties.getOrDefault(property, "")).trim();
    }

    @Override
    public Object getAsObject(String property) {
        return properties.getOrDefault(property, "");
    }

    @Override
    public boolean has(String property) {
        return properties.containsKey(property);
    }

    @Override
    public Map<String, Object> properties() {
        return Collections.unmodifiableMap(properties);
    }

    @Override
    public void set(String property, Object value) {
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

    @Override
    public Record clone() {
        GenericRecord newRecord = new GenericRecord(new HashMap<>(properties), rootIdentifier, projectId, expeditionCode, persist);
        newRecord.hasError = hasError;
        return newRecord;
    }

    private void removeFieldProperties() {
        if (properties.containsKey(Record.EXPEDITION_CODE)) {
            setExpeditionCode(String.valueOf(properties.remove(Record.EXPEDITION_CODE)));
        }
        if (properties.containsKey(Record.PROJECT_ID)) {
            String val = String.valueOf(properties.remove(Record.PROJECT_ID));
            if (projectId == 0) {
                setProjectId(Integer.parseInt(val));
            }
        }
        if (properties.containsKey(Record.ROOT_IDENTIFIER)) {
            properties.remove(Record.ROOT_IDENTIFIER);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof GenericRecord)) return false;

        GenericRecord that = (GenericRecord) o;

        if (projectId != that.projectId) return false;
        if (properties != null ? !properties.equals(that.properties) : that.properties != null) return false;
        if (rootIdentifier != null ? !rootIdentifier.equals(that.rootIdentifier) : that.rootIdentifier != null)
            return false;
        return expeditionCode != null ? expeditionCode.equals(that.expeditionCode) : that.expeditionCode == null;
    }

    @Override
    public int hashCode() {
        int result = properties != null ? properties.hashCode() : 0;
        result = 31 * result + (rootIdentifier != null ? rootIdentifier.hashCode() : 0);
        result = 31 * result + projectId;
        result = 31 * result + (expeditionCode != null ? expeditionCode.hashCode() : 0);
        return result;
    }
}
