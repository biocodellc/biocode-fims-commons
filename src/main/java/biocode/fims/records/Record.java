package biocode.fims.records;

import com.fasterxml.jackson.annotation.*;

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

    @JsonProperty
    int projectId();

    void setProjectId(int projectId);

    @JsonProperty
    String expeditionCode();

    void setExpeditionCode(String expeditionCode);

    @JsonProperty
    String rootIdentifier();

    String get(String property);

    boolean has(String property);

    @JsonAnySetter
    void set(String property, String value);

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

    Record clone();
}
