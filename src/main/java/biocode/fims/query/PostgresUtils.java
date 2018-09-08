package biocode.fims.query;

import org.apache.commons.lang3.StringUtils;

import java.util.HashMap;
import java.util.Map;

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

    public static Map<String, Object> getTableMap(int networkId, String conceptAlias) {
        if (StringUtils.isBlank(conceptAlias)) {
            throw new IllegalStateException("entity conceptAlias must not be null");
        }

        Map<String, Object> tableMap = new HashMap<>();
        tableMap.put("table", entityTable(networkId, conceptAlias));
        return tableMap;
    }
}
