package biocode.fims.query;

import biocode.fims.config.Config;
import biocode.fims.config.models.DataType;
import biocode.fims.config.models.Entity;
import biocode.fims.models.ExpeditionMetadataProperty;

import java.util.Optional;

/**
 * @author rjewing
 */
public class ExpeditionProp implements QueryColumn {
    private final Config config;
    private final String property;

    ExpeditionProp(Config config, String property) {
        this.config = config;
        this.property = property;
    }

    @Override
    public String property() {
        return property;
    }

    @Override
    public String column() {
        return "metadata";
    }

    @Override
    public String table() {
        return "expeditions";
    }

    @Override
    public DataType dataType() {
        Optional<ExpeditionMetadataProperty> expeditionMetadataProperty = config.expeditionMetadataProperties().stream()
                .filter(p -> p.getName().equalsIgnoreCase(property))
                .findFirst();

        if (expeditionMetadataProperty.isPresent()) {
            switch (expeditionMetadataProperty.get().getType()) {
                case BOOLEAN:
                    return DataType.BOOLEAN;
                case LIST:
                case STRING:
                    return DataType.STRING;
            }
        }
        return null;
    }

    @Override
    public Entity entity() {
        return null;
    }

    @Override
    public boolean isLocalIdentifier() {
        return false;
    }

    @Override
    public boolean isParentIdentifier() {
        return false;
    }
}
