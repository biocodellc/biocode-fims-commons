package biocode.fims.records;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonUnwrapped;

import java.util.Map;

/**
 * Note that implementing classes should override equals() and hashCode() methods. These methods are used
 * for removing duplicates from a RecordSet. Thus 2 Records of the same class and with the same property values
 * should be considered equal
 *
 * @author rjewing
 */
public interface Record {
    String EXPEDITION_CODE = "expeditionCode";
    String PROJECT_ID = "projectId";
    String ROOT_IDENTIFIER = "rootIdentifier";

    int projectId();

    void setProjectId(int projectId);

    String expeditionCode();

    void setExpeditionCode(String expeditionCode);

    String rootIdentifier();

    String get(String property);

    boolean has(String property);

    void set(String property, String value);

    // AnyGetter is useful for /tissues/plates/{projectId}/{plateName} endpoint,
    // but may cause issues if we want to return any other fields
    @JsonAnyGetter
    Map<String, String> properties();

    void setMetadata(RecordMetadata recordMetadata);

    /**
     * this indicates that the Record needs to be persisted.
     *
     * @return
     */
    boolean persist();

    /**
     * Should be called during validation to indicate that this record contains an Error
     * <p>
     * This will prevent this record from being persisted.
     *
     * @return
     */
    void setError();
}
