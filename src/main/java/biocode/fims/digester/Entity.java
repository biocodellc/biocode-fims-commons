package biocode.fims.digester;

import biocode.fims.fimsExceptions.FimsRuntimeException;
import biocode.fims.fimsExceptions.errorCodes.ConfigCode;
import biocode.fims.models.records.GenericRecord;
import biocode.fims.models.records.Record;
import com.fasterxml.jackson.annotation.JsonIgnore;
import org.apache.commons.lang.StringUtils;

import java.net.URI;
import java.util.*;

public class Entity {

    private final LinkedList<Attribute> attributes;
    private final Set<biocode.fims.validation.rules.Rule> rules;
    private String worksheet;
    private String uniqueKey;
    private String conceptAlias;
    private String conceptURI;
    private String conceptForwardingAddress;
    private String parentEntity;
    private boolean esNestedObject = false;
    private Class<? extends Record> recordType = GenericRecord.class;
    private URI identifier;

    public Entity(String conceptAlias) {
        this.conceptAlias = conceptAlias;
        rules = new HashSet<>();
        attributes = new LinkedList<>();
    }

    public void addAttribute(Attribute a) {
        attributes.addLast(a);
    }

    public LinkedList<Attribute> getAttributes() {
        return attributes;
    }

    public Set<biocode.fims.validation.rules.Rule> getRules() {
        return rules;
    }

    public void addRule(biocode.fims.validation.rules.Rule rule) {
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

    public String getConceptAlias() {
        return conceptAlias;
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

    @Deprecated
    public boolean isEsNestedObject() {
        return esNestedObject;
    }

    @Deprecated
    public void setEsNestedObject(boolean esNestedObject) {
        this.esNestedObject = esNestedObject;
    }

    public Class<? extends Record> getRecordType() {
        return recordType;
    }

    public void setRecordType(Class recordType) {
        this.recordType = recordType;
    }

    @JsonIgnore
    public URI getIdentifier() {
        return identifier;
    }

    public void setIdentifier(URI identifier) {
        this.identifier = identifier;
    }

    @JsonIgnore
    public boolean isValueObject() {
        return getUniqueKey() != null && getUniqueKey().contains("HASH");
    }

    @JsonIgnore
    public boolean isChildEntity() {
        return getParentEntity() != null;
    }

    public Attribute getAttribute(String column) {
        for (Attribute a: attributes) {
            if (a.getColumn().equals(column)) {
                return a;
            }
        }

        throw new FimsRuntimeException(ConfigCode.MISSING_ATTRIBUTE, 500);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Entity)) return false;

        Entity entity = (Entity) o;

        if (isEsNestedObject() != entity.isEsNestedObject()) return false;
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
        if (getRecordType() != null ? !getRecordType().equals(entity.getRecordType()) : entity.getRecordType() != null)
            return false;
        return getIdentifier() != null ? getIdentifier().equals(entity.getIdentifier()) : entity.getIdentifier() == null;
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
        result = 31 * result + (isEsNestedObject() ? 1 : 0);
        result = 31 * result + (getRecordType() != null ? getRecordType().hashCode() : 0);
        result = 31 * result + (getIdentifier() != null ? getIdentifier().hashCode() : 0);
        return result;
    }
}
