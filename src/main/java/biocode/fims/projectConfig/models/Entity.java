package biocode.fims.projectConfig.models;

import biocode.fims.fimsExceptions.FimsRuntimeException;
import biocode.fims.fimsExceptions.errorCodes.ConfigCode;
import biocode.fims.models.records.GenericRecord;
import biocode.fims.models.records.Record;
import biocode.fims.projectConfig.ProjectConfig;
import biocode.fims.serializers.EntityTypeIdResolver;
import biocode.fims.validation.rules.Rule;
import biocode.fims.validation.rules.RuleLevel;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.annotation.JsonTypeIdResolver;
import org.apache.commons.lang.StringUtils;
import org.springframework.util.Assert;

import java.util.*;
import java.util.List;

/**
 * This is the base Entity within the fims system. This class can be extended to provide a more
 * specific Entity. When extending, note that the base `Entity.isValid(config)` always returns
 * true, but can be overridden to provide additional entity specific validation that the `ProjectConfigValidator`
 * does not provide. `Entity.validationErrorMessages` can be overridden to return the List<String> of valdation
 * error messages to return if `Entity.isValid` returns false.
 * <p>
 * overriding `Entity.configure` allows subclass to dynamically configure themselves
 * <p>
 * Subclasses should override the type() method, as this is used for polymorphic deserialization
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonTypeInfo(use = JsonTypeInfo.Id.CUSTOM, include = JsonTypeInfo.As.PROPERTY, property = "type", defaultImpl = Entity.class)
@JsonTypeIdResolver(EntityTypeIdResolver.class)
@JsonIgnoreProperties(ignoreUnknown = true)
public class Entity {
    private static final String TYPE = "Entity";

    private LinkedList<Attribute> attributes;
    private LinkedHashSet<Rule> rules;
    private String worksheet;
    // note: if Entity.isChildEntity() == true, then this is
    // a composite unique key (parentEntityUniqueKey_EntityUniqueKey)
    private String uniqueKey;
    private boolean uniqueAcrossProject = false;
    private boolean hashed = false;
    private String conceptAlias;
    private String conceptURI;
    private String conceptForwardingAddress;
    private String parentEntity;
    protected Class<? extends Record> recordType = GenericRecord.class;

    // needed for jackson deserialization
    public Entity() {
        rules = new LinkedHashSet<>();
        attributes = new LinkedList<>();
    } // can make package-private after converting all configs

    public Entity(String conceptAlias, String conceptURI) {
        Assert.notNull(conceptAlias);
        Assert.notNull(conceptURI);
        this.conceptAlias = conceptAlias;
        this.conceptURI = conceptURI;
        rules = new LinkedHashSet<>();
        attributes = new LinkedList<>();
    }

    public void addAttribute(Attribute a) {
        attributes.addLast(a);
    }

    public LinkedList<Attribute> getAttributes() {
        return attributes;
    }

    public LinkedHashSet<Rule> getRules() {
        return rules;
    }

    public void addRule(Rule rule) {
        this.rules.add(rule);
    }

    public String getWorksheet() {
        return worksheet;
    }

    public void setWorksheet(String worksheet) {
        this.worksheet = worksheet;
    }

    public String getUniqueKey() {
        return uniqueKey;
    }

    /**
     * returns the uri for the column that designates unique records for this entity
     *
     * @return
     */
    @JsonIgnore
    public String getUniqueKeyURI() {
        if (StringUtils.isBlank(uniqueKey)) {
            return uniqueKey;
        }

        return attributes.stream()
                .filter(a -> uniqueKey.equals(a.getColumn()))
                .findFirst()
                .orElse(new Attribute(null, null))
                .getUri();
    }

    /**
     * column of the {@link Attribute} that designates unique records for this entity
     *
     * @param uniqueKey
     */
    public void setUniqueKey(String uniqueKey) {
        this.uniqueKey = uniqueKey;
    }

    /**
     * If true, the uniqueKey for this entity must be unique across the entire project.
     * <p>
     * By default, the uniqueKey for this entity must be unique across the expedition
     *
     * @return
     */
    public boolean getUniqueAcrossProject() {
        return uniqueAcrossProject;
    }

    public void setUniqueAcrossProject(boolean uniqueAcrossProject) {
        this.uniqueAcrossProject = uniqueAcrossProject;
    }

    /**
     * If true, the unique key is generated as a hash of all values in the record
     *
     * @return
     */
    public boolean isHashed() {
        return hashed;
    }

    public void setHashed(boolean hashed) {
        this.hashed = hashed;
    }

    public String getConceptAlias() {
        return conceptAlias;
    }

    // TODO remove after all project configurations have been converted
    public void setConceptAlias(String conceptAlias) {
        this.conceptAlias = conceptAlias;
    }

    public String getConceptURI() {
        return conceptURI;
    }

    public void setConceptURI(String conceptURI) {
        this.conceptURI = conceptURI;
    }

    public String getConceptForwardingAddress() {
        return conceptForwardingAddress;
    }

    public void setConceptForwardingAddress(String conceptForwardingAddress) {
        this.conceptForwardingAddress = conceptForwardingAddress;
    }

    public String getAttributeUri(String column) {
        return attributes.stream()
                .filter(a -> a.getColumn().equals(column))
                .findFirst()
                .map(Attribute::getUri)
                .orElse(null);
    }

    public String getAttributeColumn(String uri) {
        for (Attribute a : attributes) {
            if (a.getUri().equals(uri)) {
                return a.getColumn();
            }
        }

        return null;
    }

    public String buildChildIdentifier(String parentIdentifier, String localIdentifier) {
        return parentIdentifier + "_" + localIdentifier;
    }

    /**
     * If this entity is represented as a worksheet
     *
     * @return
     */
    @JsonIgnore
    public boolean hasWorksheet() {
        return !StringUtils.isBlank(worksheet);
    }

    @JsonIgnore
    public boolean hasWorksheet(String sheetName) {
        return hasWorksheet() && worksheet.equals(sheetName);
    }

    public String getParentEntity() {
        return parentEntity;
    }

    public void setParentEntity(String parentEntity) {
        this.parentEntity = parentEntity;
    }

    public Class<? extends Record> getRecordType() {
        return recordType;
    }

    public void setRecordType(Class<? extends Record> recordType) {
        this.recordType = recordType;
    }

    @JsonIgnore
    public boolean isChildEntity() {
        return getParentEntity() != null;
    }

    public Attribute getAttribute(String column) {
        for (Attribute a : attributes) {
            if (a.getColumn().equals(column)) {
                return a;
            }
        }

        throw new FimsRuntimeException(ConfigCode.MISSING_ATTRIBUTE, 500);
    }

    public Attribute getAttributeByUri(String uri) {
        for (Attribute a : attributes) {
            if (a.getUri().equals(uri)) {
                return a;
            }
        }

        throw new FimsRuntimeException(ConfigCode.MISSING_ATTRIBUTE, 500);
    }

    /**
     * Find the first rule matching type & level
     *
     * @param type
     * @param level
     * @param <T>
     * @return
     */
    @JsonIgnore
    public <T extends Rule> T getRule(Class<T> type, RuleLevel level) {
        for (Rule rule : rules) {
            if (rule.getClass().isAssignableFrom(type) && rule.level().equals(level)) {
                return (T) rule;
            }
        }

        return null;
    }

    public void generateUris() {
        java.util.List<String> existingUris = getExistingUris();

        for (Attribute a : attributes) {
            if (StringUtils.isBlank(a.getUri())) {

                String normalizedCol = normalizeColumn(a.getColumn());
                String uri = conceptAlias + "_" + normalizedCol;

                int i = 1;
                while (existingUris.contains(uri)) {
                    if (!existingUris.contains(uri + i)) {
                        uri = uri + i;
                        existingUris.add(uri);
                        break;
                    } else {
                        i++;
                    }
                }

                a.setUri(uri);
            }
        }
    }

    /**
     * Allows the entity to self-configure when the config is updated.
     *
     * @param config
     */
    public void configure(ProjectConfig config) {
        return;
    }

    /**
     * Allows the entity to provide custom validation
     *
     * @param config
     */
    public boolean isValid(ProjectConfig config) {
        return true;
    }

    /**
     * @return List of errorMessages from `isValid` call
     */
    public List<String> validationErrorMessages() {
        return Collections.emptyList();
    }

    /**
     * Specify if datasets for this entity can be reloaded (removing all records not in current upload).
     */
    public boolean canReload() {
        return true;
    }

    private java.util.List<String> getExistingUris() {
        java.util.List<String> uris = new ArrayList<>();

        for (Attribute a : attributes) {
            uris.add(a.getUri());
        }

        return uris;
    }

    private String normalizeColumn(String column) {
        return column.replaceAll(" ", "_").replaceAll("[^a-zA-Z0-9_]", "").toLowerCase();
    }

    public String type() {
        return TYPE;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Entity)) return false;

        Entity entity = (Entity) o;

        if (!getAttributes().equals(entity.getAttributes())) return false;
        if (!getRules().equals(entity.getRules())) return false;
        if (getWorksheet() != null ? !getWorksheet().equals(entity.getWorksheet()) : entity.getWorksheet() != null)
            return false;
        if (getUniqueKey() != null ? !getUniqueKey().equals(entity.getUniqueKey()) : entity.getUniqueKey() != null)
            return false;
        if (getConceptAlias() != null ? !getConceptAlias().equals(entity.getConceptAlias()) : entity.getConceptAlias() != null)
            return false;
        if (getConceptURI() != null ? !getConceptURI().equals(entity.getConceptURI()) : entity.getConceptURI() != null)
            return false;
        if (getConceptForwardingAddress() != null ? !getConceptForwardingAddress().equals(entity.getConceptForwardingAddress()) : entity.getConceptForwardingAddress() != null)
            return false;
        if (getParentEntity() != null ? !getParentEntity().equals(entity.getParentEntity()) : entity.getParentEntity() != null)
            return false;
        return getRecordType() != null ? getRecordType().equals(entity.getRecordType()) : entity.getRecordType() == null;
    }

    @Override
    public int hashCode() {
        int result = getAttributes().hashCode();
        result = 31 * result + getRules().hashCode();
        result = 31 * result + (getWorksheet() != null ? getWorksheet().hashCode() : 0);
        result = 31 * result + (getUniqueKey() != null ? getUniqueKey().hashCode() : 0);
        result = 31 * result + (getConceptAlias() != null ? getConceptAlias().hashCode() : 0);
        result = 31 * result + (getConceptURI() != null ? getConceptURI().hashCode() : 0);
        result = 31 * result + (getConceptForwardingAddress() != null ? getConceptForwardingAddress().hashCode() : 0);
        result = 31 * result + (getParentEntity() != null ? getParentEntity().hashCode() : 0);
        result = 31 * result + (getRecordType() != null ? getRecordType().hashCode() : 0);
        return result;
    }
}
