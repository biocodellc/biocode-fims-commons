package biocode.fims.query;

/**
 * @author rjewing
 */
public class PostgresUtils {

    public static String schema(int networkId) {
        return "network_" + networkId;
    }

    public static String entityTable(int networkId, String conceptAlias) {
        return schema(networkId) + "." + conceptAlias;
    }

    /**
     * returns entityTable AS conceptAlias expression
     *
     * @param networkId
     * @param conceptAlias
     * @return
     */
    public static String entityTableAs(int networkId, String conceptAlias) {
        return entityTable(networkId, conceptAlias) + " AS " + conceptAlias;
    }

}
