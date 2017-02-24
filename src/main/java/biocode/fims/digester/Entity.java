package biocode.fims.digester;

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

    public URI getIdentifier() {
        return identifier;
    }

    public void setIdentifier(URI identifier) {
        this.identifier = identifier;
    }

    public boolean isValueObject() {
        return getUniqueKey() != null && getUniqueKey().contains("HASH");
    }

    /**
     * Get the table.column notation
     * @return
     */
    public String getColumn() {
        return getWorksheet() + "." + getUniqueKey();
    }
}
