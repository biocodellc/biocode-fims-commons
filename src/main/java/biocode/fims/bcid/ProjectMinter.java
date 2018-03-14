package biocode.fims.bcid;

import biocode.fims.fimsExceptions.BadRequestException;
import biocode.fims.fimsExceptions.ServerErrorException;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

/**
 * Mint new expeditions.  Includes the automatic creation of a core set of entity types
 */
public class ProjectMinter {

    /**
     * The constructor defines the class-level variables used when minting Expeditions.
     * It defines a generic set of entities (process, information content, objects, agents)
     * that can be used for any expedition.
     */
    public ProjectMinter() {
    }

    /**
     * Find the BCID that denotes the validation file location for a particular expedition
     *
     * @param projectId defines the projectId to lookup
     *
     * @return returns the BCID for this expedition and conceptURI combination
     */
    public String getValidationXML(Integer projectId) {
        PreparedStatement stmt = null;
        ResultSet rs = null;
        Connection conn = BcidDatabase.getConnection();

        try {
            String query = "select \n" +
                    "validation_xml\n" +
                    "from \n" +
                    " projects\n" +
                    "where \n" +
                    "id=?";
            stmt = conn.prepareStatement(query);
            stmt.setInt(1, projectId);

            rs = stmt.executeQuery();
            rs.next();
            return rs.getString("validation_xml");
        } catch (SQLException e) {
            throw new ServerErrorException("Server Error", "Trouble getting Configuration File", e);
        } finally {
            BcidDatabase.close(conn, stmt, rs);
        }
    }

    /**
     * A utility function to get the very latest graph loads for each expedition
     * This is a public accessible function from the REST service so it only returns results that are declared as
     * public
     *
     * @param projectId pass in an project Bcid to limit the set of expeditions we are looking at
     *
     * @return
     */

    public JSONArray getLatestGraphs(int projectId, String username) {
        JSONArray graphs = new JSONArray();
        PreparedStatement stmt = null;
        ResultSet rs = null;
        Connection conn = BcidDatabase.getConnection();

        try {
            // Construct the query
            // This query is built to give us a groupwise maximum-- we want the graphs that correspond to the
            // maximum timestamp (latest) loaded for a particular expedition.
            // Help on solving this problem came from http://jan.kneschke.de/expeditions/mysql/groupwise-max/
            String sql = "select p.expedition_code as expeditionCode,p.expedition_title,b1.graph as graph,b1.ts as ts, b1.web_address as webAddress, b1.identifier as identifier, b1.id as id, p.project_id as projectId \n" +
                    "from bcids as b1, \n" +
                    "(select p.expedition_code as expeditionCode,b.graph as graph,max(b.id) as maxId, b.web_address as webAddress, b.identifier as identifer, b.id as id, p.project_id as projectId \n" +
                    "    \tfrom bcids b,expeditions p, expedition_bcids eB\n" +
                    "    \twhere eB.bcid_id=b.id\n" +
                    "    \tand eB.expedition_id=p.id\n" +
                    " and b.resource_type = 'http://purl.org/dc/dcmitype/Dataset'\n" +
                    " and b.sub_resource_type = 'FimsMetadata' \n" +
                    "    and p.project_id = ?\n" +
                    "    \tgroup by p.expedition_code) as  b2,\n" +
                    "expeditions p,  expedition_bcids eB\n" +
                    "where p.expedition_code = b2.expedition_code and b1.id = b2.maxId\n" +
                    " and eB.bcid_id=b1.id\n" +
                    " and eB.expedition_id=p.id\n" +
                    " and b1.resource_type = 'http://purl.org/dc/dcmitype/Dataset'\n" +
                    " and b1.sub_resource_type = 'FimsMetadata' " +
                    "    and p.project_id =?";

            // Enforce restriction on viewing particular bcids -- this is important for protected bcids
            if (username != null) {
                sql += "    and (p.public = 1 or p.userId = ?)";
            } else {
                sql += "    and p.public = 1";
            }
            sql += " ORDER BY expeditionTitle";

            stmt = conn.prepareStatement(sql);
            stmt.setInt(1, projectId);
            stmt.setInt(2, projectId);

            // Enforce restriction on viewing particular bcids -- this is important for protected bcids
            if (username != null) {
                Integer userId = BcidDatabase.getUserId(username);
                stmt.setInt(3, userId);
            }

            rs = stmt.executeQuery();
            while (rs.next()) {
                JSONObject graph = new JSONObject();
                // Grap the prefixes and concepts associated with this
                graph.put("expeditionCode", rs.getString("expeditionCode"));
                graph.put("expeditionTitle", rs.getString("expedition_title"));
                graph.put("ts", rs.getString("ts"));
                graph.put("identifier", rs.getString("identifier"));
                graph.put("bcidId", rs.getString("id"));
                graph.put("projectId", rs.getString("projectId"));
                graph.put("webAddress", rs.getString("webAddress"));
                graph.put("graph", rs.getString("graph"));

                graphs.add(graph);
            }
            return graphs;
        } catch (SQLException e) {
            throw new ServerErrorException("Server Error", "Trouble getting latest graphs.", e);
        } finally {
            BcidDatabase.close(conn, stmt, rs);
        }
    }

    public static void main(String args[]) {
        // See if the user owns this expedition or no
        ProjectMinter project = new ProjectMinter();
        //System.out.println(project.listProjects());
//        System.out.println("datasets = \n" + project.getMyTemplatesAndDatasets("demo"));
        System.out.println("datasets = \n" + project.getMyLatestGraphs("demo"));
    }


    /**
     * return a JSON representation of the projects that a user is a member of
     *
     * @param username
     *
     * @return
     */
    public JSONArray listUsersProjects(String username) {
        JSONArray projects = new JSONArray();
        PreparedStatement stmt = null;
        ResultSet rs = null;
        Connection conn = BcidDatabase.getConnection();

        try {
            Integer userId = BcidDatabase.getUserId(username);

            String sql = "SELECT p.id , p.project_code, p.project_title, p.validation_xml FROM projects p, user_projects u WHERE p.id = u.project_id AND u.id = '" + userId + "'";
            stmt = conn.prepareStatement(sql);

            rs = stmt.executeQuery();

            while (rs.next()) {
                JSONObject project = new JSONObject();
                project.put("projectId", rs.getString("id"));
                project.put("projectCode", rs.getString("project_code"));
                project.put("projectTitle", rs.getString("project_title"));
                project.put("validationXml", rs.getString("validation_xml"));
                projects.add(project);
            }
        } catch (SQLException e) {
            throw new ServerErrorException(e);
        } finally {
            BcidDatabase.close(conn, stmt, rs);
        }

        return projects;
    }

    /**
     * retrieve the project metadata for a given projectId and userId
     *
     * @param projectId
     * @param username
     *
     * @return
     */
    public JSONObject getMetadata(Integer projectId, String username) {
        JSONObject metadata = new JSONObject();
        PreparedStatement stmt = null;
        ResultSet rs = null;
        Connection conn = BcidDatabase.getConnection();

        try {
            Integer userId = BcidDatabase.getUserId(username);

            String sql = "SELECT project_title as title, public, validation_xml as validationXml FROM projects WHERE id=?"
                    + " AND user_id= ?";
            stmt = conn.prepareStatement(sql);
            stmt.setInt(1, projectId);
            stmt.setInt(2, userId);

            rs = stmt.executeQuery();
            if (rs.next()) {
                metadata.put("title", rs.getString("title"));
                metadata.put("public", String.valueOf(rs.getBoolean("public")));
                metadata.put("validationXml", (rs.getString("validationXml") != null) ? rs.getString("validationXml") : "");
            } else {
                throw new BadRequestException("You must be this project's admin in order to view its configuration.");
            }
        } catch (SQLException e) {
            throw new ServerErrorException("Server Error", "SQLException retrieving project configuration for projectID: " +
                    projectId, e);
        } finally {
            BcidDatabase.close(conn, stmt, rs);
        }
        return metadata;
    }
    
    public Boolean isProjectAdmin(String username, Integer projectId) {
        int userId = BcidDatabase.getUserId(username);
        return isProjectAdmin(userId, projectId);
    }

    /**
     * Check if a user is a given project's admin
     *
     * @param userId
     * @param projectId
     *
     * @return
     */
    public Boolean isProjectAdmin(Integer userId, Integer projectId) {
        PreparedStatement stmt = null;
        ResultSet rs = null;
        Connection conn = BcidDatabase.getConnection();
        try {
            String sql = "SELECT count(*) as count FROM projects WHERE user_id = ? AND id = ?";
            stmt = conn.prepareStatement(sql);
            stmt.setInt(1, userId);
            stmt.setInt(2, projectId);

            rs = stmt.executeQuery();
            rs.next();

            return rs.getInt("count") >= 1;
        } catch (SQLException e) {
            throw new ServerErrorException(e);
        } finally {
            BcidDatabase.close(conn, stmt, rs);
        }
    }

    /**
     * Remove a user from a project. Once removed, a user can no longer create/view expeditions in the project.
     *
     * @param userId
     * @param projectId
     *
     * @return
     */
    public void removeUser(Integer userId, Integer projectId) {
        PreparedStatement stmt = null;
        Connection conn = BcidDatabase.getConnection();
        try {
            String sql = "DELETE FROM user_projects WHERE user_id = ? AND project_id = ?";
            stmt = conn.prepareStatement(sql);
            stmt.setInt(1, userId);
            stmt.setInt(2, projectId);

            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new ServerErrorException("Server error while removing user", e);
        } finally {
            BcidDatabase.close(conn, stmt, null);
        }
    }

    /**
     * Add a user as a member to the project. This user can then create expeditions in this project.
     *
     * @param userId
     * @param projectId
     *
     * @return
     */
    public void addUserToProject(Integer userId, Integer projectId) {
        PreparedStatement stmt = null;
        Connection conn = BcidDatabase.getConnection();

        try {
            String insertStatement = "INSERT INTO user_projects (user_id, project_id) VALUES(?,?)";
            stmt = conn.prepareStatement(insertStatement);

            stmt.setInt(1, userId);
            stmt.setInt(2, projectId);

            stmt.execute();
        } catch (SQLException e) {
            throw new ServerErrorException("Server error while adding user to project.", e);
        } finally {
            BcidDatabase.close(conn, stmt, null);
        }
    }

    /**
     * retrieve all the members of a given project.
     *
     * @param projectId
     *
     * @return
     */
    public JSONObject getProjectUsers(Integer projectId) {
        JSONObject response = new JSONObject();
        JSONArray users = new JSONArray();
        PreparedStatement stmt = null;
        ResultSet rs = null;
        Connection conn = BcidDatabase.getConnection();

        try {
            String userProjectSql = "SELECT user_id FROM user_projects WHERE project_id = '" + projectId + "'";
            String projectSql = "SELECT project_title FROM projects WHERE id = '" + projectId + "'";
            stmt = conn.prepareStatement(projectSql);

            rs = stmt.executeQuery();
            rs.next();
            response.put("projectTitle", rs.getString("project_title"));

            BcidDatabase.close(null, stmt, rs);

            stmt = conn.prepareStatement(userProjectSql);
            rs = stmt.executeQuery();

            while (rs.next()) {
                JSONObject user = new JSONObject();
                int userId = rs.getInt("user_id");
                user.put("userId", userId);
                user.put("username", BcidDatabase.getUserName(userId));
                users.add(user);
            }
            response.put("users", users);

            return response;
        } catch (SQLException e) {
            throw new ServerErrorException("Server error retrieving project users.", e);
        } finally {
            BcidDatabase.close(conn, stmt, rs);
        }
    }

    /**
     * A utility function to get all the datasets that belong to a user
     * This function returns only Expeditions that have loaded Datasets.
     *
     * @return
     */
    public String getMyTemplatesAndDatasets(String username) {
        PreparedStatement stmt = null;
        ResultSet rs = null;
        Connection conn = BcidDatabase.getConnection();
        HashMap projectMap = new HashMap();


        // We need a username
        if (username == null) {
            throw new ServerErrorException("server error", "username can't be null");
        }
        Integer userId = BcidDatabase.getUserId(username);

        try {
            String sql1 = "SELECT e.expedition_title, p.project_title FROM expeditions e, projects p " +
                    "WHERE e.user_id = ? and p.id = e.project_id " +
                    "UNION " +
                    "SELECT e.expedition_title, p.project_title FROM expeditions e, expedition_bcids eB, projects p, bcids b " +
                    "WHERE b.user_id = ? and b.resource_type = 'http://purl.org/dc/dcmitype/Dataset'\n" +
                    " and b.sub_resource_type = 'FimsMetadata' \n" +
                    " and eB.bcid_id=b.id\n" +
                    " and e.id=eB.expedition_id\n" +
                    " and p.id=e.project_id\n" +
                    " GROUP BY e.id";

            stmt = conn.prepareStatement(sql1);
            stmt.setInt(1, userId);
            stmt.setInt(2, userId);

            rs = stmt.executeQuery();

            while (rs.next()) {
                String projectTitle = rs.getString("project_title");
                String expeditionTitle = rs.getString("expedition_title");

                if (projectTitle != null && !projectTitle.isEmpty()) {
                    // if the project isn't in the map, then add it
                    if (!projectMap.containsKey(projectTitle)) {
                        projectMap.put(projectTitle, new JSONObject());
                    }

                    // if the expedition isn't in the project then add it
                    JSONObject p = (JSONObject) projectMap.get(projectTitle);
                    if (!p.containsKey(expeditionTitle)) {
                        p.put(expeditionTitle, new JSONArray());
                    }

                }

            }

            String sql2 = "select e.expedition_code expeditionCode, " +
                    "e.expedition_title as expeditionTitle," +
                    "b.ts as ts, " +
                    "b.identifier as identifier, " +
                    "b.id as id, " +
                    "e.project_id as projectId, " +
                    "p.project_title as projectTitle \n" +
                    "from bcids b, expeditions e,  expedition_bcids eB, projects p\n" +
                    "where b.user_id = ? and b.resource_type = 'http://purl.org/dc/dcmitype/Dataset'\n" +
                    " and b.sub_resource_type = 'FimsMetadata' \n" +
                    " and eB.bcid_id=b.id\n" +
                    " and e.id=eB.expedition_id\n" +
                    " and p.id=e.project_id\n" +
                    " order by projectId, expeditionCode, ts desc";

            stmt = conn.prepareStatement(sql2);

            stmt.setInt(1, userId);

            rs = stmt.executeQuery();


            while (rs.next()) {
                JSONObject dataset = new JSONObject();

                String projectTitle = rs.getString("projectTitle");
                String expeditionTitle = rs.getString("expeditionTitle");

                // Grap the prefixes and concepts associated with this
                dataset.put("ts", rs.getString("ts"));
                dataset.put("bcidId", rs.getString("id"));
                dataset.put("identifier", rs.getString("identifier"));

                JSONObject p = (JSONObject) projectMap.get(projectTitle);

                ((JSONArray) p.get(expeditionTitle)).add(dataset);
            }
            return JSONValue.toJSONString(projectMap);
        } catch (SQLException e) {
            throw new ServerErrorException("Server Error", "Trouble getting users datasets.", e);
        } finally {
            BcidDatabase.close(conn, stmt, rs);
        }
    }

    /**
     * A utility function to get the very latest graph loads that belong to a user
     *
     * @return
     */
    public String getMyLatestGraphs(String username) {
        PreparedStatement stmt = null;
        ResultSet rs = null;
        Connection conn = BcidDatabase.getConnection();
        HashMap projectMap = new HashMap();
        JSONArray projectDatasets;

        JSONArray projects = listUsersProjects(username);

        for (Object p : projects) {
            JSONObject project = (JSONObject) p;
            projectMap.put(project.get("projectTitle"), new JSONArray());
        }

        // We need a username
        if (username == null) {
            throw new ServerErrorException("server error", "username can't be null");
        }

        try {
            // Construct the query
            // This query is built to give us a groupwise maximum-- we want the graphs that correspond to the
            // maximum timestamp (latest) loaded for a particular expedition.
            // Help on solving this problem came from http://jan.kneschke.de/expeditions/mysql/groupwise-max/
            String sql = "select e.expedition_code, e.expedition_title, b1.graph, b1.ts, b1.id as id, b1.web_address as webAddress, b1.identifier as identifier, e.project_id, e.public, p.project_title\n" +
                    "from bcids as b1, \n" +
                    "(select e.expedition_code as expeditionCode,b.graph as graph,max(b.id) as maxId, b.web_address as webAddress, b.identifier as identifier, b.id as id, e.project_id as projectId \n" +
                    "    \tfrom bcids b,expeditions e, expedition_bcids eB\n" +
                    "    \twhere eB.bcid_id=b.id\n" +
                    "    \tand eB.expedition_id=e.id\n" +
                    " and b.resource_type = 'http://purl.org/dc/dcmitype/Dataset'\n" +
                    " and b.sub_resource_type = 'FimsMetadata' \n" +
                    "    \tgroup by e.expeditionCode) as  b2,\n" +
                    "expeditions e,  expedition_bcids eB, projects p\n" +
                    "where e.expedition_code = b2.expeditionCode and b1.id = b2.maxId\n" +
                    " and eB.bcid_id=b1.id\n" +
                    " and eB.expedition_id=e.id\n" +
                    " and p.id=e.project_id\n" +
                    " and b1.resource_type = 'http://purl.org/dc/dcmitype/Dataset'\n" +
                    " and b1.sub_resource_type = 'FimsMetadata' \n" +
                    "    and e.user_id = ?";

            stmt = conn.prepareStatement(sql);
            Integer userId = BcidDatabase.getUserId(username);
            stmt.setInt(1, userId);

            rs = stmt.executeQuery();
            while (rs.next()) {
                JSONObject dataset = new JSONObject();
                String projectTitle = rs.getString("project_title");

                // Grap the prefixes and concepts associated with this
                dataset.put("expeditionCode", rs.getString("expedition_code"));
                dataset.put("expeditionTitle", rs.getString("expedition_title"));
                dataset.put("ts", rs.getString("ts"));
                dataset.put("bcidId", rs.getString("id"));
                dataset.put("projectId", rs.getString("project_id"));
                dataset.put("graph", rs.getString("graph"));
                dataset.put("public", rs.getString("public"));
                dataset.put("identifier", rs.getString("identifier"));
                dataset.put("webAddress", rs.getString("webAddress"));


                if (projectTitle != null && !projectTitle.isEmpty()) {
                    projectDatasets = (JSONArray) projectMap.get(projectTitle);
                    // TODO What should we do if a projectTitle shows up that wasn't before fetched? ignore it? or add the project?
                    if (projectDatasets == null) {
                        projectDatasets = new JSONArray();
                    }
                    projectDatasets.add(dataset);
                    projectMap.put(projectTitle, projectDatasets);
                }
            }

            return JSONValue.toJSONString(projectMap);
        } catch (SQLException e) {
            throw new ServerErrorException("Server Error", "Trouble getting latest graphs.", e);
        } finally {
            BcidDatabase.close(conn, stmt, rs);
        }
    }

    /**
     * save a template generator configuration
     *
     * @param configName
     * @param projectId
     * @param userId
     * @param checkedOptions
     */
    public void saveTemplateConfig(String configName, Integer projectId, Integer userId, List<String> checkedOptions) {
        PreparedStatement stmt = null;
        Connection conn = BcidDatabase.getConnection();

        try {
            String insertStatement = "INSERT INTO template_configs (user_id, project_id, config_name, config) " +
                    "VALUES(?,?,?,?)";
            stmt = conn.prepareStatement(insertStatement);

            stmt.setInt(1, userId);
            stmt.setInt(2, projectId);
            stmt.setString(3, configName);
            stmt.setString(4, JSONValue.toJSONString(checkedOptions));

            stmt.execute();
        } catch (SQLException e) {
            throw new ServerErrorException("Server error while saving template config.", e);
        } finally {
            BcidDatabase.close(conn, stmt, null);
        }
    }

    /**
     * check if a config exists
     *
     * @param configName
     * @param projectId
     *
     * @return
     */
    public boolean templateConfigExists(String configName, Integer projectId) {
        PreparedStatement stmt = null;
        ResultSet rs = null;
        Connection conn = BcidDatabase.getConnection();
        try {
            String sql = "SELECT count(*) as count " +
                    "FROM template_configs " +
                    "WHERE config_name = ? and project_id = ?";
            stmt = conn.prepareStatement(sql);
            stmt.setString(1, configName);
            stmt.setInt(2, projectId);

            rs = stmt.executeQuery();
            rs.next();

            if (rs.getInt("count") > 0) {
                return true;
            }
            return false;
        } catch (SQLException e) {
            throw new ServerErrorException(e);
        } finally {
            BcidDatabase.close(conn, stmt, rs);
        }
    }

    /**
     * check if a user owns the config
     */
    public boolean usersTemplateConfig(String configName, Integer projectId, Integer userId) {
        PreparedStatement stmt = null;
        ResultSet rs = null;
        Connection conn = BcidDatabase.getConnection();
        try {
            String sql = "SELECT count(*) as count " +
                    "FROM template_configs " +
                    "WHERE config_name = ? and project_id = ? and user_id = ?";
            stmt = conn.prepareStatement(sql);
            stmt.setString(1, configName);
            stmt.setInt(2, projectId);
            stmt.setInt(3, userId);

            rs = stmt.executeQuery();
            rs.next();

            if (rs.getInt("count") > 0) {
                return true;
            }
            return false;
        } catch (SQLException e) {
            throw new ServerErrorException(e);
        } finally {
            BcidDatabase.close(conn, stmt, rs);
        }
    }

    /**
     * get the template generator configuration for the given project
     *
     * @param projectId
     *
     * @return
     */
    public JSONArray getTemplateConfigs(Integer projectId) {
        PreparedStatement stmt = null;
        ResultSet rs = null;
        Connection conn = BcidDatabase.getConnection();

        JSONArray configNames = new JSONArray();

        configNames.add("Default");

        try {
            String sql = "SELECT config_name FROM template_configs WHERE project_id = ?";
            stmt = conn.prepareStatement(sql);
            stmt.setInt(1, projectId);

            rs = stmt.executeQuery();
            while (rs.next()) {
                configNames.add(rs.getString("configName"));
            }
        } catch (SQLException e) {
            throw new ServerErrorException("Server Error", "SQLException retrieving template configurations for projectID: " +
                    projectId, e);
        } finally {
            BcidDatabase.close(conn, stmt, rs);
        }
        return configNames;
    }

    /**
     * get a specific template generator configuration
     *
     * @param configName
     * @param projectId
     *
     * @return
     */
    public JSONObject getTemplateConfig(String configName, Integer projectId) {
        PreparedStatement stmt = null;
        ResultSet rs = null;
        Connection conn = BcidDatabase.getConnection();

        JSONObject obj = new JSONObject();

        try {
            String sql = "SELECT config FROM template_configs WHERE project_id = ? AND config_name = ?";
            stmt = conn.prepareStatement(sql);
            stmt.setInt(1, projectId);
            stmt.setString(2, configName);

            rs = stmt.executeQuery();
            if (rs.next()) {
                obj.put("checkedOptions", JSONValue.parse(rs.getString("config")));
            } else {
                obj.put("error", configName + " template configuration not found.");
            }
        } catch (SQLException e) {
            throw new ServerErrorException("Server Error", "SQLException retrieving template config.", e);
        } finally {
            BcidDatabase.close(conn, stmt, rs);
        }

        return obj;
    }

    public void updateTemplateConfig(String configName, Integer projectId, Integer userId, List<String> checkedOptions) {
        PreparedStatement stmt = null;
        Connection conn = BcidDatabase.getConnection();

        try {
            String updateStatement = "UPDATE template_configs SET config = ? WHERE " +
                    "user_id = ? AND project_id = ? and config_name = ?";
            stmt = conn.prepareStatement(updateStatement);

            stmt.setString(1, JSONValue.toJSONString(checkedOptions));
            stmt.setInt(2, userId);
            stmt.setInt(3, projectId);
            stmt.setString(4, configName);

            stmt.execute();
        } catch (SQLException e) {
            throw new ServerErrorException("Server error while updating template config.", e);
        } finally {
            BcidDatabase.close(conn, stmt, null);
        }
    }

    public void removeTemplateConfig(String configName, Integer projectId) {
        PreparedStatement stmt = null;
        Connection conn = BcidDatabase.getConnection();

        try {
            String sql = "DELETE FROM template_configs WHERE project_id = ? AND config_name = ?";

            stmt = conn.prepareStatement(sql);
            stmt.setInt(1, projectId);
            stmt.setString(2, configName);

            stmt.execute();
        } catch (SQLException e) {
            throw new ServerErrorException("Server error while removing template config.");
        } finally {
            BcidDatabase.close(conn, stmt, null);
        }
    }

    /**
     * Discover if a user belongs to an project
     *
     * @param userId
     * @param projectId
     *
     * @return
     */
    public boolean userExistsInProject(Integer userId, Integer projectId) {
        String selectString = "SELECT count(*) as count FROM user_projects WHERE user_id = ? AND project_id = ?";
        PreparedStatement stmt = null;
        ResultSet rs = null;
        Connection conn = BcidDatabase.getConnection();

        try {
            stmt = conn.prepareStatement(selectString);

            stmt.setInt(1, userId);
            stmt.setInt(2, projectId);

            rs = stmt.executeQuery();
            rs.next();
            return rs.getInt("count") >= 1;
        } catch (SQLException e) {
            throw new ServerErrorException(e);
        } finally {
            BcidDatabase.close(conn, stmt, rs);
        }
    }
}

