package biocode.fims.models;

import org.springframework.util.Assert;

/**
 * Class to represent a metadata property for an {@link Expedition}.
 *
 * @author rjewing
 */
public class ExpeditionMetadataProperty {
    private String name;
    private boolean required;

    // needed for jackson deserialization
    ExpeditionMetadataProperty() {}

    /**
     * @param name      The name of the metadata property
     * @param required  Is the property required
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
}

