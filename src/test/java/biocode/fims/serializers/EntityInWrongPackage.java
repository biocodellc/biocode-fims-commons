package biocode.fims.serializers;

import biocode.fims.digester.Entity;

/**
 * @author rjewing
 */
public class EntityInWrongPackage extends Entity {
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
