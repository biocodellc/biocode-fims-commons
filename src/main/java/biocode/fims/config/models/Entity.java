package biocode.fims.config.models;

import biocode.fims.config.Config;
import biocode.fims.records.Record;
import biocode.fims.serializers.EntityTypeIdResolver;
import biocode.fims.validation.rules.Rule;
import biocode.fims.validation.rules.RuleLevel;
import com.fasterxml.jackson.annotation.*;
import com.fasterxml.jackson.databind.annotation.JsonTypeIdResolver;

import java.util.*;

/**
 * @author rjewing
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonTypeInfo(use = JsonTypeInfo.Id.CUSTOM, include = JsonTypeInfo.As.PROPERTY, property = "type", defaultImpl = DefaultEntity.class)
@JsonTypeIdResolver(EntityTypeIdResolver.class)
@JsonIgnoreProperties(ignoreUnknown = true)
public interface Entity {
    void addAttributes(Collection<Attribute> attributes);

    void addAttribute(Attribute a);

    LinkedList<Attribute> getAttributes();

    @JsonProperty
    LinkedHashSet<Rule> getRules();

    @JsonSetter("rules")
    void addRules(Collection<Rule> rules);

    void addRule(Rule rule);

    String getWorksheet();

    void setWorksheet(String worksheet);

    String getUniqueKey();

    @JsonIgnore
    String getUniqueKeyURI();

    void setUniqueKey(String uniqueKey);

    boolean getUniqueAcrossProject();

    void setUniqueAcrossProject(boolean uniqueAcrossProject);

    boolean isHashed();

    void setHashed(boolean hashed);

    String getConceptAlias();

    String getConceptURI();

    void setConceptURI(String conceptURI);

    String getAttributeUri(String column);

    String getAttributeColumn(String uri);

    @JsonIgnore
    boolean hasWorksheet();

    String getParentEntity();

    void setParentEntity(String parentEntity);

    Class<? extends Record> getRecordType();

    void setRecordType(Class<? extends Record> recordType);

    @JsonIgnore
    boolean isChildEntity();

    Attribute getAttribute(String column);

    Attribute getAttributeByUri(String uri);

    @JsonIgnore
    <T extends Rule> T getRule(Class<T> type, RuleLevel level);

    void generateUris();

    void addDefaultRules(Config config);

    void configure(Config config);

    boolean isValid(Config config);

    java.util.List<String> validationErrorMessages();

    boolean canReload();

    /**
     * Additional user configurable entity properties that need to be persisted
     * when converting to/from a {@link biocode.fims.config.project.models.ProjectEntity}
     *
     * @return
     */
    @JsonIgnore
    Map<String, Object> additionalProps();

    @JsonIgnore
    void setAdditionalProps(Map<String, Object> props);

    @JsonIgnore
    Entity clone();

    String type();
}
