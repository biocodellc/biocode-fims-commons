package biocode.fims.models.records;

import java.util.List;

/**
 * Note that implementing classes should override equals() and hashCode() methods. These methods are used
 * for removing duplicates from a RecordSet. Thus 2 Records of the same class and with the same property values
 * should be considered equal
 *
 * @author rjewing
 */
public interface Record {
    String get(String property);

    boolean has(String property);

    void set(String property, String value);

    List<Record> all();

    void setMetadata(RecordMetadata recordMetadata);
}
