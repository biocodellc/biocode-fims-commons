package biocode.fims.models.records;

import java.util.Map;

/**
 * Note that implementing classes should override equals() and hashCode() methods. These methods are used
 * for removing duplicates from a RecordSet. Thus 2 Records of the same class and with the same property values
 * should be considered equal
 *
 * @author rjewing
 */
public interface Record {
    int projectId();

    String expeditionCode();

    String rootIdentifier();

    String get(String property);

    boolean has(String property);

    void set(String property, String value);

    Map<String, String> properties();

    void setMetadata(RecordMetadata recordMetadata);

    /**
     * this indicates that the Record needs to be persisted.
     * @return
     */
    boolean persist();
}
