package biocode.fims.models;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.springframework.util.Assert;

import java.util.List;

/**
 * Class to represent a metadata property for an {@link Expedition}.
 *
 * @author rjewing
 */
public class ExpeditionMetadataProperty {
    private String name;
    private boolean required;
    private Type type = Type.STRING;
    private List<String> values;
    private boolean isNetworkProp;

    // needed for jackson deserialization
    ExpeditionMetadataProperty() {
    }

    /**
     * @param name     The name of the metadata property
     * @param required Is the property required
     */
    public ExpeditionMetadataProperty(String name, boolean required) {
        Assert.notNull(name);
        this.name = name;
        this.required = required;
    }

    public String getName() {
        return name;
    }

    public boolean isRequired() {
        return required;
    }

    public Type getType() {
        return type;
    }

    public List<String> getValues() {
        return values;
    }

    @JsonIgnore
    public boolean isNetworkProp() {
        return isNetworkProp;
    }

    public void setNetworkProp(boolean networkProp) {
        isNetworkProp = networkProp;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ExpeditionMetadataProperty)) return false;

        ExpeditionMetadataProperty that = (ExpeditionMetadataProperty) o;

        if (isRequired() != that.isRequired()) return false;
        if (!getName().equals(that.getName())) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = getName().hashCode();
        result = 31 * result + (isRequired() ? 1 : 0);
        return result;
    }

    public enum Type {
        STRING, LIST, BOOLEAN
    }
}

