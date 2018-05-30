package biocode.fims.projectConfig.models;

/**
 * @author rjewing
 */
public class TestEntity extends Entity {
    public static final String UNIQUE_KEY = "test entity unique key";
    public static final String TYPE = "TEST";

    @Override
    public String getUniqueKey() {
        return UNIQUE_KEY;
    }

    @Override
    public String type() {
        return TYPE;
    }
}
