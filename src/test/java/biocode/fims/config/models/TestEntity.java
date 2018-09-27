package biocode.fims.config.models;

/**
 * @author rjewing
 */
public class TestEntity extends DefaultEntity {
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
