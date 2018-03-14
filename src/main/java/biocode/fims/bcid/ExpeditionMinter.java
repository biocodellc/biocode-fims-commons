package biocode.fims.bcid;

import biocode.fims.fimsExceptions.ForbiddenRequestException;
import biocode.fims.fimsExceptions.ServerErrorException;
import biocode.fims.settings.SettingsManager;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import javax.ws.rs.core.MultivaluedMap;
import java.sql.*;
import java.util.*;

/**
 * Mint new expeditions.  Includes the automatic creation of a core set of entity types
 */
public class ExpeditionMinter {
    private SettingsManager sm = SettingsManager.getInstance();

    /**
     * @param expeditionCode
     * @param ProjectId
     *
     * @return
     */
    public Boolean expeditionExistsInProject(String expeditionCode, Integer ProjectId) {
        PreparedStatement stmt = null;
        ResultSet rs = null;
        Connection conn = BcidDatabase.getConnection();
        try {
            String sql = "select id from expeditions " +
                    "where expedition_code = ? AND " +
                    "project_id = ?";
            stmt = conn.prepareStatement(sql);

            stmt.setString(1, expeditionCode);
            stmt.setInt(2, ProjectId);

            rs = stmt.executeQuery();
            if (rs.next()) return true;
        } catch (SQLException e) {
            throw new ServerErrorException(e);
        } finally {
            BcidDatabase.close(conn, stmt, rs);
        }
        return false;
    }


    /**
     * Discover if a user owns this expedition or not
     *
     * @param userId
     * @param expeditionCode
     *
     * @return
     */
    public boolean userOwnsExpedition(Integer userId, String expeditionCode, Integer projectId) {
        PreparedStatement stmt = null;
        ResultSet rs = null;
        Connection conn = BcidDatabase.getConnection();
        try {

            String sql = "SELECT " +
                    "   count(*) as count " +
                    "FROM " +
                    "   expeditions " +
                    "WHERE " +
                    "   expedition_code = ? AND " +
                    "   user_id = ? AND " +
                    "   project_id = ?";
            stmt = conn.prepareStatement(sql);

            stmt.setString(1, expeditionCode);
            stmt.setInt(2, userId);
            stmt.setInt(3, projectId);

            rs = stmt.executeQuery();
            rs.next();
            if (rs.getInt("count") < 1)
                return false;
            else
                return true;
        } catch (SQLException e) {
            throw new ServerErrorException(e);
        } finally {
            BcidDatabase.close(conn, stmt, rs);
        }
    }

    /**
     * Get Metadata on a named graph
     *
     * @param graphName
     *
     * @return
     */
    public JSONObject getGraphMetadata(String graphName) {
        JSONObject metadata = new JSONObject();
        PreparedStatement stmt = null;
        ResultSet rs = null;
        Connection conn = BcidDatabase.getConnection();

        try {
            String sql =
                    "SELECT " +
                            " b.graph as graph, " +
                            " a.project_id as projectId, " +
                            " u.username as expeditionOwner, " +
                            " u2.username as uploader," +
                            " b.created as timestamp," +
                            " b.identifier, " +
                            " b.resource_type as resourceType," +
                            " b.finalCopy as finalCopy," +
                            " a.expedition_code as expeditionCode, " +
                            " a.expedition_title as expeditionTitle, " +
                            " a.public as public " +
                            "FROM " +
                            " expeditions a, expedition_bcids eB, bcids b, users u, users u2 " +
                            "WHERE" +
                            " u2.id = b.user_id AND " +
                            " u.id = a.user_id AND " +
                            " a.id = eB.expedition_id AND " +
                            " eB.bcid_id = b.id AND \n" +
                            " b.graph = ?";
            stmt = conn.prepareStatement(sql);

            stmt.setString(1, graphName);
            rs = stmt.executeQuery();
            if (rs.next()) {
                // Grab the prefixes and concepts associated with this
                metadata.put("graph", rs.getString("graph"));
                metadata.put("projectId", rs.getInt("projectId"));
                metadata.put("expeditionOwner", rs.getString("expeditionOwner"));
                metadata.put("uploader", rs.getString("uploader"));
                metadata.put("timestamp", rs.getString("timestamp"));
                metadata.put("identifier", rs.getString("identifier"));
                metadata.put("resourceType", rs.getString("resourceType"));
                metadata.put("isPublic", rs.getBoolean("public"));
                metadata.put("expeditionCode", rs.getString("expeditionCode"));
                metadata.put("expeditionTitle", rs.getString("expeditionTitle"));
            }
        } catch (SQLException e) {
            throw new ServerErrorException(e);
        } finally {
            BcidDatabase.close(conn, stmt, rs);
        }
        return metadata;
    }

    /**
     * Return an JSONArray of the expeditions associated with a project. Includes who owns the expedition,
     * the expedition title, and whether the expedition is public.  This information is returned as information
     * typically viewed by an Admin who wants to see details about what datasets are as part of an expedition
     *
     * @param projectId
     * @param username  the project's admins username
     *
     * @return
     */
    public JSONArray getExpeditions(Integer projectId, String username) {
        JSONArray expeditions = new JSONArray();

        PreparedStatement stmt = null;
        ResultSet rs = null;
        Connection conn = BcidDatabase.getConnection();

        ProjectMinter p = new ProjectMinter();
        try {
            Integer userId = BcidDatabase.getUserId(username);

            if (!p.isProjectAdmin(userId, projectId)) {
                throw new ForbiddenRequestException("You must be this project's admin to view its expeditions.");
            }

            String sql = "SELECT max(b.id) bcidId, e.expedition_title, e.id, e.public, u.username \n" +
                    " FROM expeditions as e, users as u, bcids b, expedition_bcids eB \n" +
                    " WHERE \n" +
                    " \te.project_id = ? \n" +
                    " \tAND u.id = e.user_id \n" +
                    " \tAND b.id = eB.bcid_id\n" +
                    " \tAND eB.expedition_id = e.id \n" +
                    " \tAND b.resource_type = 'http://purl.org/dc/dcmitype/Dataset' \n" +
                    " \tAND b.sub_resource_type = 'FimsMetadata' \n" +
                    " GROUP BY eB.expedition_id";
            stmt = conn.prepareStatement(sql);
            stmt.setInt(1, projectId);

            rs = stmt.executeQuery();

            while (rs.next()) {
                JSONObject expedition = new JSONObject();
                expedition.put("username", rs.getString("username"));
                expedition.put("expeditionTitle", rs.getString("expedition_title"));
                expedition.put("expeditionId", rs.getString("id"));
                expedition.put("public", rs.getBoolean("public"));
                expeditions.add(expedition);
            }

        } catch (SQLException e) {
            throw new ServerErrorException(e);
        } finally {
            BcidDatabase.close(conn, stmt, rs);
        }
        return expeditions;
    }

    /**
     * Update the public status of a specific expedition
     */
    public Boolean updateExpeditionPublicStatus(Integer userId, String expeditionCode, Integer projectId, Boolean publicStatus) {
        // Check to see that this user owns this expedition
        if (!userOwnsExpedition(userId, expeditionCode, projectId)) {
            throw new ForbiddenRequestException("You must be the owner of this expedition to update the public status.");
        }

        PreparedStatement updateStatement = null;
        Connection conn = BcidDatabase.getConnection();

        try {
            String updateString = "UPDATE expeditions SET public = ?" +
                    " WHERE expedition_code = '" + expeditionCode + "' AND project_id = " + projectId;

            updateStatement = conn.prepareStatement(updateString);
            updateStatement.setBoolean(1, publicStatus);

            updateStatement.execute();
            if (updateStatement.getUpdateCount() > 0)
                return true;
            else
                return false;

        } catch (SQLException e) {
            throw new ServerErrorException("Server Error", "SQLException while updating expedition public status.", e);
        } finally {
            BcidDatabase.close(conn, updateStatement, null);
        }
    }

    /**
     * Update the public attribute of each expedition in the expeditions MultivaluedMap
     *
     * @param expeditions
     * @param projectId
     *
     * @return
     */
    public void updateExpeditionsPublicStatus(MultivaluedMap<String, String> expeditions, Integer projectId) {
        List<String> updateExpeditions = new ArrayList<String>();
        PreparedStatement stmt = null;
        ResultSet rs = null;
        Connection conn = BcidDatabase.getConnection();

        try {
            String sql = "SELECT id, public FROM expeditions WHERE project_id = ?";
            stmt = conn.prepareStatement(sql);
            stmt.setInt(1, projectId);

            rs = stmt.executeQuery();

            while (rs.next()) {
                String expeditionId = rs.getString("id");
                if (expeditions.containsKey(expeditionId) &&
                        !expeditions.getFirst(expeditionId).equals(String.valueOf(rs.getBoolean("public")))) {
                    updateExpeditions.add(expeditionId);
                }
            }

            if (!updateExpeditions.isEmpty()) {
                String updateString = "UPDATE expeditions SET" +
                        " public = CASE WHEN public ='0' THEN '1' WHEN public = '1' THEN '0' END" +
                        " WHERE id IN (" + updateExpeditions.toString().replaceAll("[\\[\\]]", "") + ")";

                BcidDatabase.close(null, stmt, null);
                stmt = conn.prepareStatement(updateString);

                stmt.executeUpdate();
            }

        } catch (SQLException e) {
            throw new ServerErrorException("Db error while updating Expeditions public status.", e);
        } finally {
            BcidDatabase.close(conn, stmt, rs);
        }
    }

    public boolean isPublic(String expeditionCode, Integer projectId) {
        PreparedStatement stmt = null;
        ResultSet rs = null;
        Connection conn = BcidDatabase.getConnection();
        try {
            String sql = "SELECT public FROM expeditions WHERE expedition_code = ? AND project_id = ?";
            stmt = conn.prepareStatement(sql);

            stmt.setString(1, expeditionCode);
            stmt.setInt(2, projectId);

            rs = stmt.executeQuery();
            rs.next();
            return rs.getBoolean("public");
        } catch (SQLException e) {
            throw new ServerErrorException(e);
        } finally {
            BcidDatabase.close(conn, stmt, rs);
        }
    }
}
