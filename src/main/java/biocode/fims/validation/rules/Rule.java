package biocode.fims.validation.rules;

import biocode.fims.digester.Entity;
import biocode.fims.models.records.RecordSet;
import biocode.fims.projectConfig.ProjectConfig;
import biocode.fims.projectConfig.ProjectConfigValidator;
import biocode.fims.validation.messages.EntityMessages;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.annotation.JsonTypeIdResolver;

import java.util.List;

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
    RuleLevel level();

    /**
     * Method to be called in {@link ProjectConfigValidator}. This allows Rule implementations to specify any
     * additional validation.
     *
     * @param messages error messages to be presented to user
     */
    @JsonIgnore
    boolean validConfiguration(List<String> messages, Entity entity);

    /**
     * if {@link Rule#run(RecordSet, EntityMessages)} has failed, this tells us if it was an {@link RuleLevel#ERROR}.
     * @return
     */
    @JsonIgnore
    boolean hasError();

    @JsonIgnore
    void setProjectConfig(ProjectConfig config);
}
