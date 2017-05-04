package biocode.fims.digester;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.net.URI;

/**
 * Entity representation
 */
public class Entity extends AbstractEntity {

    private String parentEntity;
    private boolean esNestedObject = false;
    private URI identifier;

    public String getParentEntity() {
        return parentEntity;
    }

    public void setParentEntity(String parentEntity) {
        this.parentEntity = parentEntity;
    }

    public boolean isEsNestedObject() {
        return esNestedObject;
    }

    public void setEsNestedObject(boolean esNestedObject) {
        this.esNestedObject = esNestedObject;
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

    /**
     * Get the table.column notation
     * @return
     */
    @JsonIgnore
    public String getColumn() {
        return getWorksheet() + "." + getUniqueKey();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Entity)) return false;

        Entity entity = (Entity) o;

        if (isEsNestedObject() != entity.isEsNestedObject()) return false;
        if (getIdentifier() != null ? !getIdentifier().equals(entity.getIdentifier()) : entity.getIdentifier() != null)
            return false;
        if (getParentEntity() != null ? !getParentEntity().equals(entity.getParentEntity()) : entity.getParentEntity() != null)
            return false;
        return super.equals(o);
    }

    @Override
    public int hashCode() {
        int result = (isEsNestedObject() ? 1 : 0);
        result = 31 * result + (getIdentifier() != null ? getIdentifier().hashCode() : 0);
        result = 31 * result + (getParentEntity() != null ? getParentEntity().hashCode() : 0);
        return result * super.hashCode();
    }
}
