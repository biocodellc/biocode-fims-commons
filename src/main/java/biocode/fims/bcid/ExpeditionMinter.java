package biocode.fims.bcid;

import biocode.fims.entities.BcidTmp;
import biocode.fims.fimsExceptions.ForbiddenRequestException;
import biocode.fims.fimsExceptions.ServerErrorException;
import biocode.fims.repositories.BcidTmpRepository;
import biocode.fims.settings.SettingsManager;
import biocode.fims.utils.SpringApplicationContext;
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
     * Attach an individual URI reference to a expedition
     *
     * @param expeditionCode
     * @param identifier
     */
    public void attachReferenceToExpedition(String expeditionCode, String identifier, Integer projectId) {
        Integer expeditionId = getExpeditionId(expeditionCode, projectId);
        BcidTmpRepository bcidTmpRepository = (BcidTmpRepository) SpringApplicationContext.getBean("bcidTmpRepository");
        BcidTmp bcidTmp = bcidTmpRepository.findByIdentifier(identifier);

        attachReferenceToExpedition(expeditionId, bcidTmp.getBcidId());
    }

    private void attachReferenceToExpedition(Integer expeditionId, Integer bcidId) {

        String insertString = "INSERT INTO expeditionBcids " +
                "(expeditionId, bcidId) " +
                "values (?,?)";

        PreparedStatement insertStatement = null;
        Connection conn = BcidDatabase.getConnection();
        try {
            insertStatement = conn.prepareStatement(insertString);
            insertStatement.setInt(1, expeditionId);
            insertStatement.setInt(2, bcidId);
            insertStatement.execute();
        } catch (SQLException e) {
            throw new ServerErrorException("Db error attaching Reference to Expedition", e);
        } finally {
            BcidDatabase.close(conn, insertStatement, null);
        }
    }

    private Integer getExpeditionId(String expeditionCode, Integer projectId) {
        PreparedStatement stmt = null;
        ResultSet rs = null;
        Connection conn = BcidDatabase.getConnection();
        try {
            String sql = "SELECT expeditionId " +
                    "FROM expeditions " +
                    "WHERE expeditionCode = ? AND " +
                    "projectId = ?";
            stmt = conn.prepareStatement(sql);

            stmt.setString(1, expeditionCode);
            stmt.setInt(2, projectId);

            rs = stmt.executeQuery();
            rs.next();
            return rs.getInt("expeditionId");
        } catch (SQLException e) {
            throw new ServerErrorException("Db error while retrieving expeditionId",
                    "SQLException while retrieving expeditionId from expeditions table with expeditionCode: " +
                            expeditionCode + " and projectId: " + projectId, e);
        } finally {
            BcidDatabase.close(conn, stmt, rs);
        }
    }

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
            String sql = "select expeditionId from expeditions " +
                    "where expeditionCode = ? && " +
                    "projectId = ?";
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
     * Verify that a user owns this expedition
     *
     * @param userId
     * @param expeditionId
     *
     * @return
     */
    public boolean userOwnsExpedition(Integer userId, Integer expeditionId) {
        PreparedStatement stmt = null;
        ResultSet rs = null;
        Connection conn = BcidDatabase.getConnection();
        try {
            String sql = "SELECT " +
                    "   count(*) as count " +
                    "FROM " +
                    "   expeditions " +
                    "WHERE " +
                    "   expeditionId= ? && " +
                    "   userId = ?";
            stmt = conn.prepareStatement(sql);

            stmt.setInt(1, expeditionId);
            stmt.setInt(2, userId);

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
                    "   expeditionCode= ? && " +
                    "   userId = ? && " +
                    "   projectId = ?";
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
                            " a.projectId as projectId, " +
                            " u.username as expeditionOwner, " +
                            " u2.username as uploader," +
                            " b.ts as timestamp," +
                            " b.identifier, " +
                            " b.resourceType as resourceType," +
                            " b.finalCopy as finalCopy," +
                            " a.expeditionCode as expeditionCode, " +
                            " a.expeditionTitle as expeditionTitle, " +
                            " a.public as public " +
                            "FROM " +
                            " expeditions a, expeditionBcids eB, bcids b, users u, users u2 " +
                            "WHERE" +
                            " u2.userId = b.userId && " +
                            " u.userId = a.userId && " +
                            " a.expeditionId = eB.expeditionId && " +
                            " eB.bcidId = b.bcidId && \n" +
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
                metadata.put("identifier", rs.getString("b.identifier"));
                metadata.put("resourceType", rs.getString("resourceType"));
                metadata.put("finalCopy", rs.getBoolean("finalCopy"));
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

    public static void main(String args[]) {
        try {
            System.out.println("init ...");
            // See if the user owns this expedition or no
            ExpeditionMinter expedition = new ExpeditionMinter();
            //System.out.println(expedition.getGraphMetadata("_qNK_fuHVbRSTNvA_8pG.xlsx"));
            System.out.println("starting ...");
//            System.out.println(expedition.getExpeditionss(9,"trizna"));
            //System.out.println(expedition.getFimsMetadataDatasets(30));

            System.out.println("ending ...");
            //System.out.println(expedition.listExpeditions(8,"mwangiwangui25@gmail.com"));
            //expedition.checkExpeditionCodeValid("JBD_foo-))");
            //    System.out.println("Configuration File for project = " +expedition.getValidationXML(1));
            /*
            if (expedition.expeditionExistsInProject("DEMOH", 1)) {
                System.out.println("expedition exists in project");
            } else {
                System.out.println("expedition does not exist in project");
            }
            */
            /*System.out.println(expedition.getDeepRoots("HDIM"));

            if (expedition.userOwnsExpedition(8, "DEMOG")) {
                System.out.println("YES the user owns this expedition");
            } else {
                System.out.println("NO the user does not own this expedition");
            }

*/
            // System.out.println(expedition.getLatestGraphsByExpedition(1));
            // Test associating a BCID to a expedition
            /*
            expedition.attachReferenceToExpedition("DEMOH", "ark:/21547/Fu2");
            */

            // Test creating a expedition

            /*
            Integer expeditionId = expedition.mint(
                    "TEST214",
                    "Test creating expedition under an project for which it already exists",
                    8, 5, false);

            */
            System.out.println(expedition.getMetadata(334));


            //System.out.println(p.expeditionTable("demo"));
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }
    /**
     * return the expedition metadata given the projectId and expeditionCode
     *
     * @param projectId
     * @param expeditionCode
     *
     * @return
     */
    public JSONObject getMetadata(Integer projectId, String expeditionCode) {
        JSONObject metadata = new JSONObject();
        PreparedStatement stmt = null;
        ResultSet rs = null;
        Connection conn = BcidDatabase.getConnection();

        try {

            String sql = "SELECT b.identifier, e.public, e.expeditionId, e.expeditionTitle " +
                    "FROM bcids b, expeditionBcids eB, expeditions e " +
                    "WHERE b.bcidId = eB.bcidId && eB.expeditionId = e.expeditionId && e.expeditionCode = ? and e.projectId = ? " +
            //        " AND b.resourceType not like '%esource%' " +
            //        " ORDER BY b.ts ASC LIMIT 1";
            " and b.resourceType = \"http://purl.org/dc/dcmitype/Collection\"";
            stmt = conn.prepareStatement(sql);

            stmt.setString(1, expeditionCode);
            stmt.setInt(2, projectId);

            rs = stmt.executeQuery();
            if (rs.next()) {
                metadata.put("identifier", rs.getString("b.identifier"));
                metadata.put("public", rs.getBoolean("e.public"));
                metadata.put("expeditionCode", expeditionCode);
                metadata.put("projectId", projectId);
                metadata.put("expeditionTitle", rs.getString("e.expeditionTitle"));
                metadata.put("expeditionId", rs.getString("e.expeditionId"));
            }
        } catch (SQLException e) {
            throw new ServerErrorException(e);
        } finally {
            BcidDatabase.close(conn, stmt, rs);
        }
        return metadata;
    }

    /**
     * Return the expedition's metadata
     *
     * @param expeditionId
     *
     * @return
     */
    public JSONObject getMetadata(Integer expeditionId) {
        JSONObject metadata = new JSONObject();
        PreparedStatement stmt = null;
        ResultSet rs = null;
        Connection conn = BcidDatabase.getConnection();

        try {

            String sql = "SELECT b.identifier, e.public, e.expeditionCode, e.projectId, e.expeditionTitle, b.webAddress, b.resourceType " +
                    "FROM bcids b, expeditionBcids eB, expeditions e " +
                    "WHERE b.bcidId = eB.bcidId && eB.expeditionId = e.expeditionId && e.expeditionId = ? " +
                   // " AND b.resourceType not like '%esource%' " +
                   // " ORDER BY b.ts ASC LIMIT 1";
             " and b.resourceType = \"http://purl.org/dc/dcmitype/Collection\"";
            stmt = conn.prepareStatement(sql);

            stmt.setInt(1, expeditionId);

            rs = stmt.executeQuery();
            if (rs.next()) {
                metadata.put("identifier", rs.getString("b.identifier"));
                metadata.put("public", rs.getBoolean("e.public"));
                metadata.put("expeditionCode", rs.getString("e.expeditionCode"));
                metadata.put("projectId", rs.getString("e.projectId"));
                metadata.put("expeditionTitle", rs.getString("e.expeditionTitle"));
                metadata.put("webAddress", rs.getString("webAddress"));
                metadata.put("resourceType", rs.getString("b.resourceType"));
                metadata.put("expeditionId", expeditionId);
            }
        } catch (SQLException e) {
            throw new ServerErrorException(e);
        } finally {
            BcidDatabase.close(conn, stmt, rs);
        }
        return metadata;
    }

    /**
     * get the expedition's resources
     *
     * @param expeditionId
     *
     * @return
     */
    public ArrayList<JSONObject> getResources(Integer expeditionId) {
        ArrayList<JSONObject> resources = new ArrayList<>();

        PreparedStatement stmt = null;
        ResultSet rs = null;
        Connection conn = BcidDatabase.getConnection();
        ResourceTypes rts = new ResourceTypes();

        try {
            String sql = "SELECT b.identifier, b.resourceType " +
                    "FROM bcids b, expeditionBcids eB " +
                    "WHERE b.bcidId = eB.bcidId && eB.expeditionId = ?";
            stmt = conn.prepareStatement(sql);

            stmt.setInt(1, expeditionId);

            ResourceTypes rt = new ResourceTypes();

            rs = stmt.executeQuery();
            while (rs.next()) {
                String rtString;
                JSONObject resource = new JSONObject();
                ResourceType resourceType = rt.get(rs.getString("b.resourceType"));
                if (resourceType != null) {
                    rtString = resourceType.string;
                } else {
                    rtString = rs.getString("b.resourceType");
                }

                // if the resourceType is a dataset or collection, don't add to table
                if (rts.get(1).equals(resourceType) || rts.get(38).equals(resourceType)) {
                    continue;
                }

                resource.put("identifier", rs.getString("b.identifier"));
                resource.put("resourceType", rtString);
                // only add the resourceTypeUri if http: is specified under resource type
                if (rs.getString("b.resourceType").contains("http:")) {
                    resource.put("resourceTypeUri", rs.getString("b.resourceType"));
                } else {
                    resource.put("resourceTypeUri", "");
                }
                resources.add(resource);
            }
        } catch (SQLException e) {
            throw new ServerErrorException(e);
        } finally {
            BcidDatabase.close(conn, stmt, rs);
        }
        return resources;
    }

    /**
     * retrieve the expedition's datasets
     *
     * @param expeditionId
     *
     * @return
     */
    public ArrayList<JSONObject> getDatasets(Integer expeditionId) {
        ArrayList<JSONObject> datasets = new ArrayList();
        PreparedStatement stmt = null;
        ResultSet rs = null;
        Connection conn = BcidDatabase.getConnection();

        try {
            String sql = "SELECT b.ts, b.identifier, b.resourceType, b.title " +
                    "FROM bcids b, expeditionBcids eB, expeditions e " +
                    "WHERE b.bcidId = eB.bcidId && eB.expeditionId = ? && e.expeditionId = eB.expeditionId " +
                    "AND b.resourceType = \"http://purl.org/dc/dcmitype/Dataset\" " +
                    "AND b.subResourceType = \"FimsMetadata\" " +
                    "ORDER BY b.ts DESC";
            stmt = conn.prepareStatement(sql);
            stmt.setInt(1, expeditionId);

            rs = stmt.executeQuery();
            while (rs.next()) {
                JSONObject dataset = new JSONObject();
                dataset.put("ts", rs.getTimestamp("b.ts").toString());
                dataset.put("identifier", rs.getString("b.identifier"));
                dataset.put("resourceType", rs.getString("resourceType"));
                dataset.put("title", rs.getString("title"));
                datasets.add(dataset);
            }
        } catch (SQLException e) {
            throw new ServerErrorException(e);
        } finally {
            BcidDatabase.close(conn, stmt, rs);
        }
        return datasets;
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

            String sql = "SELECT max(b.bcidId) bcidId, e.expeditionTitle, e.expeditionId, e.public, u.username \n" +
                    " FROM expeditions as e, users as u, bcids b, expeditionBcids eB \n" +
                    " WHERE \n" +
                    " \te.projectId = ? \n" +
                    " \tAND u.userId = e.userId \n" +
                    " \tAND b.bcidId = eB.bcidId\n" +
                    " \tAND eB.expeditionId = e.expeditionId \n" +
                    " \tAND b.resourceType = \"http://purl.org/dc/dcmitype/Dataset\" \n" +
                    " \tAND b.subResourceType = \"FimsMetadata\" \n" +
                    " GROUP BY eB.expeditionId";
            stmt = conn.prepareStatement(sql);
            stmt.setInt(1, projectId);

            rs = stmt.executeQuery();

            while (rs.next()) {
                JSONObject expedition = new JSONObject();
                expedition.put("username", rs.getString("u.username"));
                expedition.put("expeditionTitle", rs.getString("e.expeditionTitle"));
                expedition.put("expeditionId", rs.getString("e.expeditionId"));
                expedition.put("public", rs.getBoolean("e.public"));
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
                    " WHERE expeditionCode = \"" + expeditionCode + "\" AND projectId = " + projectId;

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
            String sql = "SELECT expeditionId, public FROM expeditions WHERE projectId = ?";
            stmt = conn.prepareStatement(sql);
            stmt.setInt(1, projectId);

            rs = stmt.executeQuery();

            while (rs.next()) {
                String expeditionId = rs.getString("expeditionId");
                if (expeditions.containsKey(expeditionId) &&
                        !expeditions.getFirst(expeditionId).equals(String.valueOf(rs.getBoolean("public")))) {
                    updateExpeditions.add(expeditionId);
                }
            }

            if (!updateExpeditions.isEmpty()) {
                String updateString = "UPDATE expeditions SET" +
                        " public = CASE WHEN public ='0' THEN '1' WHEN public = '1' THEN '0' END" +
                        " WHERE expeditionId IN (" + updateExpeditions.toString().replaceAll("[\\[\\]]", "") + ")";

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
            String sql = "SELECT public FROM expeditions WHERE expeditionCode = ? AND projectId = ?";
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
