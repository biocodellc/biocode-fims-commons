package biocode.fims.models;

import org.springframework.util.Assert;

/**
 * Class to represent a metadata property for an {@link Expedition}.
 *
 * @author rjewing
 */
public class ExpeditionMetadataProperty {
    private final String name;
    private final boolean required;

    /**
     * @param name      The name of the metadata property
     * @param required  Is the property required
     */
    public ExpeditionMetadataProperty(String name, boolean required) {
        Assert.notNull(name);
        this.name = name;
        this.required = required;
    }

    public String name() {
        return name;
    }

    public boolean isRequired() {
        return required;
    }
}

