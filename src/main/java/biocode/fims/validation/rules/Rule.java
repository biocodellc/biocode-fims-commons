package biocode.fims.validation.rules;

import biocode.fims.models.records.RecordSet;
import biocode.fims.renderers.EntityMessages;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.annotation.JsonTypeIdResolver;

/**
 * @author rjewing
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.CUSTOM, include = JsonTypeInfo.As.PROPERTY, property = "name")
@JsonTypeIdResolver(RuleTypeIdResolver.class)
public interface Rule {

    @JsonIgnore
    String name();

    @JsonIgnore
    boolean run(RecordSet recordSet, EntityMessages messages);

    @JsonProperty
    void setColumn(String column);

    @JsonProperty
    String column();

    @JsonProperty
    void setLevel(RuleLevel level);

    @JsonProperty
    RuleLevel level();
}
