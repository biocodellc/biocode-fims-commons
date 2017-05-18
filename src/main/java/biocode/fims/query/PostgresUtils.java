package biocode.fims.query;

/**
 * @author rjewing
 */
public class PostgresUtils {

    public static String schema(int projectId) {
        return "project_" + projectId;
    }

    public static String entityTable(int projectId, String conceptAlias) {
        return schema(projectId) + "." + conceptAlias;
    }

    /**
     * returns entityTable AS conceptAlias expression
     *
     * @param projectId
     * @param conceptAlias
     * @return
     */
    public static String entityTableAs(int projectId, String conceptAlias) {
        return entityTable(projectId, conceptAlias) + " AS " + conceptAlias;
    }

}
