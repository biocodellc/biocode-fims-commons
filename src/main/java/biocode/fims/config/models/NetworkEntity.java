package biocode.fims.config.models;

import biocode.fims.config.Config;
import biocode.fims.records.Record;
import biocode.fims.validation.rules.Rule;
import biocode.fims.validation.rules.RuleLevel;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSetter;

import java.util.*;
import java.util.List;

/**
 * @author rjewing
 */
public class NetworkEntity implements Entity {
    private static final String TYPE = "NetworkEntity";
    private Entity entity;

    private NetworkEntity() {
    } // needed for EntityTypeIdResolver

    public NetworkEntity(Entity entity) {
        this.entity = entity;
        entity.getRules().forEach(r -> r.setNetworkRule(true));
    }

    @Override
    public void addAttribute(Attribute a) {
        entity.addAttribute(a);
    }

    @Override
    public LinkedList<Attribute> getAttributes() {
        return entity.getAttributes();
    }

    @Override
    @JsonProperty
    public LinkedHashSet<Rule> getRules() {
        return entity.getRules();
    }

    @Override
    @JsonSetter("rules")
    public void addRules(Collection<Rule> rules) {
        rules.forEach(r -> r.setNetworkRule(true));
        entity.addRules(rules);
    }

    @Override
    public void addRule(Rule rule) {
        rule.setNetworkRule(true);
        entity.addRule(rule);
    }

    @Override
    public String getWorksheet() {
        return entity.getWorksheet();
    }

    @Override
    public void setWorksheet(String worksheet) {
        entity.setWorksheet(worksheet);
    }

    @Override
    public String getUniqueKey() {
        return entity.getUniqueKey();
    }

    @Override
    @JsonIgnore
    public String getUniqueKeyURI() {
        return entity.getUniqueKeyURI();
    }

    @Override
    public void setUniqueKey(String uniqueKey) {
        entity.setUniqueKey(uniqueKey);
    }

    @Override
    public boolean getUniqueAcrossProject() {
        return entity.getUniqueAcrossProject();
    }

    @Override
    public void setUniqueAcrossProject(boolean uniqueAcrossProject) {
        entity.setUniqueAcrossProject(uniqueAcrossProject);
    }

    @Override
    public boolean isHashed() {
        return entity.isHashed();
    }

    @Override
    public void setHashed(boolean hashed) {
        entity.setHashed(hashed);
    }

    @Override
    public String getConceptAlias() {
        return entity.getConceptAlias();
    }

    @Override
    public String getConceptURI() {
        return entity.getConceptURI();
    }

    @Override
    public void setConceptURI(String conceptURI) {
        entity.setConceptURI(conceptURI);
    }

    @Override
    public String getAttributeUri(String column) {
        return entity.getAttributeUri(column);
    }

    @Override
    public String getAttributeColumn(String uri) {
        return entity.getAttributeColumn(uri);
    }

    @Override
    @JsonIgnore
    public boolean hasWorksheet() {
        return entity.hasWorksheet();
    }

    @Override
    public String getParentEntity() {
        return entity.getParentEntity();
    }

    @Override
    public void setParentEntity(String parentEntity) {
        entity.setParentEntity(parentEntity);
    }

    @Override
    public Class<? extends Record> getRecordType() {
        return entity.getRecordType();
    }

    @Override
    public void setRecordType(Class<? extends Record> recordType) {
        entity.setRecordType(recordType);
    }

    @Override
    @JsonIgnore
    public boolean isChildEntity() {
        return entity.isChildEntity();
    }

    @Override
    public Attribute getAttribute(String column) {
        return entity.getAttribute(column);
    }

    @Override
    public Attribute getAttributeByUri(String uri) {
        return entity.getAttributeByUri(uri);
    }

    @Override
    @JsonIgnore
    public <T extends Rule> T getRule(Class<T> type, RuleLevel level) {
        return entity.getRule(type, level);
    }

    @Override
    public void generateUris() {
        entity.generateUris();
    }

    @Override
    public void addDefaultRules(Config config) {
        entity.addDefaultRules(config);
    }

    @Override
    public void configure(Config config) {
        entity.configure(config);
    }

    @Override
    public boolean isValid(Config config) {
        return entity.isValid(config);
    }

    @Override
    public List<String> validationErrorMessages() {
        return entity.validationErrorMessages();
    }

    @Override
    public boolean canReload() {
        return entity.canReload();
    }

    @Override
    public String type() {
        // entity will be null in EntityTypeIdResolver
        return entity == null ? TYPE : entity.type();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof NetworkEntity)) return false;

        NetworkEntity that = (NetworkEntity) o;

        return Objects.equals(entity, that.entity);
    }

    @Override
    public int hashCode() {
        return entity != null ? entity.hashCode() : 0;
    }
}
