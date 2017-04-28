package biocode.fims.digester;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.net.URI;

/**
 * Entity representation
 */
public class Entity extends AbstractEntity {

    private boolean esNestedObject = false;
    private URI identifier;

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
        return super.equals(o);
    }

    @Override
    public int hashCode() {
        int result = (isEsNestedObject() ? 1 : 0);
        result = 31 * result + (getIdentifier() != null ? getIdentifier().hashCode() : 0);
        return result * super.hashCode();
    }
}
