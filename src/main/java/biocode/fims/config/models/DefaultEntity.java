package biocode.fims.config.models;

import biocode.fims.config.Config;
import biocode.fims.config.network.NetworkConfig;
import biocode.fims.fimsExceptions.FimsRuntimeException;
import biocode.fims.fimsExceptions.errorCodes.ConfigCode;
import biocode.fims.models.dataTypes.JacksonUtil;
import biocode.fims.records.GenericRecord;
import biocode.fims.records.Record;
import biocode.fims.validation.rules.*;
import com.fasterxml.jackson.annotation.*;
import org.apache.commons.lang3.StringUtils;
import org.springframework.util.Assert;

import java.util.*;
import java.util.List;

/**
 * This is the default Entity within the fims system. This class can be extended to provide a more
 * specific Entity. When extending, note that the base `Entity.isValid(config)` always returns
 * true, but can be overridden to provide additional entity specific validation that the `ConfigValidator`
 * does not provide. `Entity.validationErrorMessages` can be overridden to return the List<String> of validation
 * error messages to return if `Entity.isValid` returns false.
 * <p>
 * overriding `Entity.configure` allows subclass to dynamically configure themselves
 * <p>
 * Subclasses should override the type() method, as this is used for polymorphic deserialization
 * Subclasses should override the clone() method, as this is used create a copy
 */
public class DefaultEntity implements Entity {
    private static final String TYPE = "DefaultEntity";

    // All available network attributes for entity
    private LinkedList<Attribute> attributes;
    // minimal set of rules for the network
    // projects can extend
    private Rules rules;

    // set by network
    private String conceptAlias;
    private String conceptURI;
    private String parentEntity;
    protected Class<? extends Record> recordType = GenericRecord.class;

    // defaults
    private String worksheet;
    // note: if Entity.isChildEntity() == true, then this is
    // a composite unique key (parentEntityUniqueKey_EntityUniqueKey)
    private String uniqueKey;
    private boolean uniqueAcrossProject = false;
    private boolean hashed = false;


    // needed for jackson deserialization && EntityTypeIdResolver
    protected DefaultEntity() {
        rules = new Rules();
        attributes = new LinkedList<>();
    }

    public DefaultEntity(String conceptAlias, String conceptURI) {
        Assert.notNull(conceptAlias);
        Assert.notNull(conceptURI);
        this.conceptAlias = conceptAlias;
        this.conceptURI = conceptURI;
        rules = new Rules();
        attributes = new LinkedList<>();
    }

    @Override
    public void addAttributes(Collection<Attribute> attributes) {
        attributes.addAll(attributes);
    }

    @Override
    public void addAttribute(Attribute a) {
        attributes.addLast(a);
    }

    @Override
    public LinkedList<Attribute> getAttributes() {
        return attributes;
    }

    @Override
    @JsonProperty
    public LinkedHashSet<Rule> getRules() {
        return rules.get();
    }

    @Override
    @JsonSetter("rules")
    public void addRules(Collection<Rule> rules) {
        this.rules.addAll(rules);
    }

    @Override
    public void addRule(Rule rule) {
        this.rules.add(rule);
    }

    @Override
    public String getWorksheet() {
        return worksheet;
    }

    @Override
    public void setWorksheet(String worksheet) {
        this.worksheet = worksheet;
    }

    @Override
    public String getUniqueKey() {
        return uniqueKey;
    }

    /**
     * returns the uri for the column that designates unique records for this entity
     *
     * @return
     */
    @Override
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
    @Override
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
    @Override
    public boolean getUniqueAcrossProject() {
        return uniqueAcrossProject;
    }

    @Override
    public void setUniqueAcrossProject(boolean uniqueAcrossProject) {
        this.uniqueAcrossProject = uniqueAcrossProject;
    }

    /**
     * If true, the unique key is generated as a hash of all values in the record
     *
     * @return
     */
    @Override
    public boolean isHashed() {
        return hashed;
    }

    @Override
    public void setHashed(boolean hashed) {
        this.hashed = hashed;
    }

    @Override
    public String getConceptAlias() {
        return conceptAlias;
    }

    @Override
    public String getConceptURI() {
        return conceptURI;
    }

    @Override
    public void setConceptURI(String conceptURI) {
        this.conceptURI = conceptURI;
    }

    @Override
    public String getAttributeUri(String column) {
        return attributes.stream()
                .filter(a -> a.getColumn().equals(column))
                .findFirst()
                .map(Attribute::getUri)
                .orElse(null);
    }

    @Override
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
    @Override
    public boolean hasWorksheet() {
        return !StringUtils.isBlank(worksheet);
    }

    @Override
    public String getParentEntity() {
        return parentEntity;
    }

    @Override
    public void setParentEntity(String parentEntity) {
        this.parentEntity = parentEntity;
    }

    @Override
    public Class<? extends Record> getRecordType() {
        return recordType;
    }

    @Override
    public void setRecordType(Class<? extends Record> recordType) {
        this.recordType = recordType;
    }

    @Override
    public boolean isChildEntity() {
        return getParentEntity() != null;
    }

    @Override
    public Attribute getAttribute(String column) {
        for (Attribute a : attributes) {
            if (a.getColumn().equals(column)) {
                return a;
            }
        }

        throw new FimsRuntimeException(ConfigCode.MISSING_ATTRIBUTE, 500, column);
    }

    @Override
    public Attribute getAttributeByUri(String uri) {
        for (Attribute a : attributes) {
            if (a.getUri().equals(uri)) {
                return a;
            }
        }

        throw new FimsRuntimeException(ConfigCode.MISSING_ATTRIBUTE, 500, uri);
    }

    /**
     * Find the first rule matching type & level
     *
     * @param type
     * @param level
     * @param <T>
     * @return
     */
    @Override
    public <T extends Rule> T getRule(Class<T> type, RuleLevel level) {
        for (Rule rule : rules) {
            if (rule.getClass().isAssignableFrom(type) && rule.level().equals(level)) {
                return (T) rule;
            }
        }

        return null;
    }

    @Override
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
     * Add default rules to this entity
     *
     * @param config
     */
    @Override
    public void addDefaultRules(Config config) {
        addRule(new ValidDataTypeFormatRule());

        // don't add the following rules to the network config b/c the projects configs
        // may choose to use a different uniqueKey the defined in the network
        if (!(config instanceof NetworkConfig)) {
            addRule(new ValidForURIRule(getUniqueKey(), RuleLevel.ERROR));

            RequiredValueRule requiredValueRule = getRule(RequiredValueRule.class, RuleLevel.ERROR);

            if (requiredValueRule == null) {
                requiredValueRule = new RequiredValueRule(new LinkedHashSet<>(), RuleLevel.ERROR);
                addRule(requiredValueRule);
            }

            requiredValueRule.addColumn(getUniqueKey());

            if (isChildEntity()) {
                Entity parentEntity = config.entity(getParentEntity());
                requiredValueRule.addColumn(parentEntity.getUniqueKey());
            }
            addRule(new UniqueValueRule(getUniqueKey(), getUniqueAcrossProject(), RuleLevel.ERROR));
        }

        if (isChildEntity()) {
            addRule(new ValidParentIdentifiersRule());
        }
    }

    /**
     * Allows the entity to self-configure when the config is updated.
     *
     * @param config
     */
    @Override
    public void configure(Config config) {
        return;
    }

    /**
     * Allows the entity to provide custom validation
     *
     * @param config
     */
    @Override
    public boolean isValid(Config config) {
        return true;
    }

    /**
     * @return List of errorMessages from `isValid` call
     */
    @Override
    public List<String> validationErrorMessages() {
        return Collections.emptyList();
    }

    /**
     * Specify if datasets for this entity can be reloaded (removing all records not in current upload).
     */
    @Override
    public boolean canReload() {
        return true;
    }

    @Override
    public Map<String, Object> additionalProps() {
        return Collections.emptyMap();
    }

    @Override
    public void setAdditionalProps(Map<String, Object> props) {}

    @Override
    public Entity clone() {
        return clone(new DefaultEntity(conceptAlias, conceptURI));
    }

    protected Entity clone(Entity entity) {
        rules.forEach(r -> {
            // TODO create a Rule method clone()
            // hacky way to make a copy of the rule
            Rule newR = JacksonUtil.fromString(
                    JacksonUtil.toString(r),
                    r.getClass()
            );
            entity.addRule(newR);
        });
        attributes.forEach(a -> entity.addAttribute(a.clone()));

        entity.setParentEntity(parentEntity);
        entity.setRecordType(recordType);

        entity.setWorksheet(worksheet);
        entity.setUniqueKey(uniqueKey);
        entity.setUniqueAcrossProject(uniqueAcrossProject);
        entity.setHashed(hashed);

        return entity;
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

    @Override
    public String type() {
        return TYPE;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof DefaultEntity)) return false;

        DefaultEntity entity = (DefaultEntity) o;

        if (getUniqueAcrossProject() != entity.getUniqueAcrossProject()) return false;
        if (isHashed() != entity.isHashed()) return false;
        if (getAttributes() != null ? !getAttributes().equals(entity.getAttributes()) : entity.getAttributes() != null)
            return false;
        if (rules != null ? !rules.equals(entity.rules) : entity.rules != null) return false;
        if (getWorksheet() != null ? !getWorksheet().equals(entity.getWorksheet()) : entity.getWorksheet() != null)
            return false;
        if (getUniqueKey() != null ? !getUniqueKey().equals(entity.getUniqueKey()) : entity.getUniqueKey() != null)
            return false;
        if (getConceptAlias() != null ? !getConceptAlias().equals(entity.getConceptAlias()) : entity.getConceptAlias() != null)
            return false;
        if (getConceptURI() != null ? !getConceptURI().equals(entity.getConceptURI()) : entity.getConceptURI() != null)
            return false;
        if (getParentEntity() != null ? !getParentEntity().equals(entity.getParentEntity()) : entity.getParentEntity() != null)
            return false;
        return getRecordType() != null ? getRecordType().equals(entity.getRecordType()) : entity.getRecordType() == null;
    }

    @Override
    public int hashCode() {
        int result = getAttributes() != null ? getAttributes().hashCode() : 0;
        result = 31 * result + (rules != null ? rules.hashCode() : 0);
        result = 31 * result + (getWorksheet() != null ? getWorksheet().hashCode() : 0);
        result = 31 * result + (getUniqueKey() != null ? getUniqueKey().hashCode() : 0);
        result = 31 * result + (getUniqueAcrossProject() ? 1 : 0);
        result = 31 * result + (isHashed() ? 1 : 0);
        result = 31 * result + (getConceptAlias() != null ? getConceptAlias().hashCode() : 0);
        result = 31 * result + (getConceptURI() != null ? getConceptURI().hashCode() : 0);
        result = 31 * result + (getParentEntity() != null ? getParentEntity().hashCode() : 0);
        result = 31 * result + (getRecordType() != null ? getRecordType().hashCode() : 0);
        return result;
    }
}
