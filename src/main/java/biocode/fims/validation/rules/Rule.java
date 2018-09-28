package biocode.fims.validation.rules;

import biocode.fims.config.models.Attribute;
import biocode.fims.config.models.Entity;
import biocode.fims.config.project.ProjectConfig;
import biocode.fims.records.RecordSet;
import biocode.fims.config.ConfigValidator;
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
     * Method to be called in {@link ConfigValidator}. This allows Rule implementations to specify any
     * additional validation.
     *
     * @param messages error messages to be presented to user
     */
    @JsonIgnore
    boolean validConfiguration(List<String> messages, Entity entity);

    /**
     * if {@link Rule#run(RecordSet, EntityMessages)} has failed, this tells us if it was an {@link RuleLevel#ERROR}.
     *
     * @return
     */
    @JsonIgnore
    boolean hasError();

    @JsonIgnore
    boolean isNetworkRule();

    @JsonIgnore
    void setNetworkRule(boolean isNetworkRule);

    /**
     * Attempt to merge the Rule (r) into this Rule instance
     *
     * @param r Rule to attempt to merge
     * @return whether or not the rule was successfully merged
     */
    @JsonIgnore
    boolean mergeRule(Rule r);

    /**
     * Is the rule contained within this rule? Useful after mergingRules to check if a Rule is still exists
     *
     * @param r
     * @return
     */
    @JsonIgnore
    boolean contains(Rule r);

    /**
     * Get a rule instance for a project.
     *
     * Transforms this rule into an applicable rule for the provided columns/requiredColumns.
     * If the rule uses a column no in either list, then this rule is not applicable.
     * If the rule uses multiple columns, a modified rule instance will be returned that
     * only uses the provided columns if possible.
     *
     * @return Rule or null if the rule isn't applicable to the provided columns
     */
    @JsonIgnore
    Rule toProjectRule(List<String> columns);

    @JsonIgnore
    void setProjectConfig(ProjectConfig config);
}
