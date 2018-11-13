package biocode.fims.query;

import biocode.fims.config.models.DataType;
import biocode.fims.config.models.Entity;

/**
 * @author rjewing
 */
public interface QueryColumn {
    String property();

    String column();

    /**
     * The table to be queried
     * @return
     */
    String table();

    DataType dataType();

    Entity entity();

    boolean isLocalIdentifier();

    boolean isParentIdentifier();
}
