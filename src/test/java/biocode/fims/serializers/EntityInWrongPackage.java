package biocode.fims.serializers;

import biocode.fims.config.models.DefaultEntity;

/**
 * @author rjewing
 */
public class EntityInWrongPackage extends DefaultEntity {
    public static final String UNIQUE_KEY = "invalid entity";
    public static final String TYPE = "INVALID_TEST";

    @Override
    public String getUniqueKey() {
        return UNIQUE_KEY;
    }

    @Override
    public String type() {
        return TYPE;
    }
}
