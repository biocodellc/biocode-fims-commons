package biocode.fims.bcid;

import biocode.fims.auth.Authenticator;
import biocode.fims.auth.Authorizer;
import biocode.fims.auth.oauth2.OAuthProvider;
import biocode.fims.fimsExceptions.BadRequestException;
import biocode.fims.fimsExceptions.ServerErrorException;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Enumeration;
import java.util.Hashtable;

/**
 * Class to manage user creation and profile information.
 */
public class UserMinter {
    protected Connection conn;
    private Database db;

    public UserMinter() {
        db = new Database();
        conn = db.getConn();
    }
     public void close() {
         db.close();
     }

    /**
     * create a new user given their profile information and add them to a project
     * @param userInfo
     * @param projectId
     * @return
     */
    public void createUser(Hashtable<String, String> userInfo, Integer projectId) {
        Authenticator auth = new Authenticator();
        auth.createUser(userInfo);
        auth.close();
        // add user to project
        Integer userId = db.getUserId(userInfo.get("username"));
        ProjectMinter p = new ProjectMinter();

        p.addUserToProject(userId, projectId);
        p.close();
        return;
    }

    /**
     * retrieve a user's profile information
     * @param username
     * @return
     */
    public JSONObject getUserProfile(String username) {
        JSONObject profile = new JSONObject();
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            String selectStatement = "Select institution, email, firstName, lastName from users where username = ?";
            stmt = conn.prepareStatement(selectStatement);

            stmt.setString(1, username);

            rs = stmt.executeQuery();

            if (rs.next()) {
                profile.put("firstName", rs.getString("firstName"));
                profile.put("lastName", rs.getString("lastName"));
                profile.put("email", rs.getString("email"));
                profile.put("institution", rs.getString("institution"));
                profile.put("username", username);
            }
            return profile;
        } catch (SQLException e) {
            throw new ServerErrorException(e);
        } finally {
            db.close(stmt, rs);
        }

    }

    /**
     * lookup the user's institution
     * @param username
     * @return
     */
    public String getInstitution(String username) {
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            String selectStatement = "Select institution from users where username = ?";
            stmt = conn.prepareStatement(selectStatement);

            stmt.setString(1, username);

            rs = stmt.executeQuery();

            if (rs.next()) {
                return rs.getString("institution");
            }
        } catch (SQLException e) {
            throw new ServerErrorException(e);
        } finally {
            db.close(stmt, rs);
        }
        return null;
    }

    /**
     * lookup the user's email
     * @param username
     * @return
     */
    public String getEmail(String username) {
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            String selectStatement = "Select email from users where username = ?";
            stmt = conn.prepareStatement(selectStatement);

            stmt.setString(1, username);

            rs = stmt.executeQuery();

            if (rs.next()) {
                return rs.getString("email");
            }
        } catch (SQLException e) {
            throw new ServerErrorException(e);
        } finally {
            db.close(stmt, rs);
        }
        return null;
    }

    /**
     * lookup the user's first name
     * @param username
     * @return
     */
    public String getFirstName(String username) {
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            String selectStatement = "Select firstName from users where username = ?";
            stmt = conn.prepareStatement(selectStatement);

            stmt.setString(1, username);
            rs = stmt.executeQuery();

            if (rs.next()) {
                return rs.getString("firstName");
            }
        } catch (SQLException e) {
            throw new ServerErrorException(e);
        } finally {
            db.close(stmt, rs);
        }
        return null;
    }

    /**
     * lookup the user's first name
     * @param username
     * @return
     */
    public String getLastName(String username) {
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            String selectStatement = "Select lastName from users where username = ?";
            stmt = conn.prepareStatement(selectStatement);

            stmt.setString(1, username);
            rs = stmt.executeQuery();

            if (rs.next()) {
                return rs.getString("lastName");
            }
        } catch (SQLException e) {
            throw new ServerErrorException(e);
        } finally {
            db.close(stmt, rs);
        }
        return null;
    }

    /**
     * return a JSON representation of a user's profile for oAuth client apps
     * @param token
     * @return
     */
    public JSONObject getOauthProfile(String token) {
        OAuthProvider p = new OAuthProvider();
        PreparedStatement stmt = null;
        ResultSet rs = null;

        String username = p.validateToken(token);
        p.close();
        if (username != null) {
            Integer userId = db.getUserId(username);
            try {
                String sql = "SELECT lastName, firstName, email, institution, hasSetPassword " +
                        "FROM users WHERE userId = ?";
                stmt = conn.prepareStatement(sql);

                stmt.setInt(1, userId);
                rs = stmt.executeQuery();

                if (rs.next()) {
                    JSONObject profile = new JSONObject();
                    profile.put("firstName", rs.getString("firstName"));
                    profile.put("lastName", rs.getString("lastName"));
                    profile.put("email", rs.getString("email"));
                    profile.put("institution", rs.getString("institution"));
                    profile.put("hasSetPassword", rs.getString("hasSetPassword"));
                    profile.put("userId", userId);
                    profile.put("username", username);
                    Authorizer authorizer = new Authorizer();
                    profile.put("projectAdmin", authorizer.userProjectAdmin(username));
                    authorizer.close();

                    return profile;
                }
            } catch (SQLException e) {
                throw new ServerErrorException(e);
            } finally {
                db.close(stmt, rs);
            }
        }

        throw new BadRequestException("invalid_grant", "access token is not valid");
    }

    /**
     * check if a username already exists
     * @param username
     * @return
     */
    public Boolean checkUsernameExists(String username) {
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            String sql = "SELECT count(*) as count FROM users WHERE username = ?";
            stmt = conn.prepareStatement(sql);

            stmt.setString(1, username);

            rs = stmt.executeQuery();
            rs.next();
            return rs.getInt("count") >= 1;

        } catch (SQLException e) {
            throw new ServerErrorException(e);
        } finally {
            db.close(stmt, rs);
        }
    }

    /**
     * update a users profile
     * @param info
     * @param username
     * @return
     */
    public Boolean updateProfile(Hashtable<String, String> info, String username) {
        String updateString = "UPDATE users SET ";

        // Dynamically create our UPDATE statement depending on which fields the user wants to update
        for (Enumeration e = info.keys(); e.hasMoreElements();){
            String key = e.nextElement().toString();
            updateString += key + " = ?";

            if (e.hasMoreElements()) {
                updateString += ", ";
            }
            else {
                updateString += " WHERE username = ?;";
            }
        }

        PreparedStatement stmt = null;

        try {
            stmt = conn.prepareStatement(updateString);

            // place the parametrized values into the SQL statement
            {
                int i = 1;
                for (Enumeration e = info.keys(); e.hasMoreElements();) {
                    String key = e.nextElement().toString();
                    stmt.setString(i, info.get(key));
                    i++;

                    if (!e.hasMoreElements()) {
                        stmt.setString(i, username);
                    }
                }
            }

            Integer result = stmt.executeUpdate();

            // result should be '1', if not, an error occurred during the UPDATE statement
            return result == 1;
        } catch (SQLException e) {
            throw new ServerErrorException(e);
        } finally {
            db.close(stmt, null);
        }
    }

    /**
     * retrieve all users
     * @return
     */
    public JSONArray getUsers() {
        JSONArray users = new JSONArray();
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            String sql = "SELECT userId FROM users";
            stmt = conn.prepareStatement(sql);

            rs = stmt.executeQuery();

            while (rs.next()) {
                JSONObject user = new JSONObject();
                Integer userId = rs.getInt("userId");
                user.put("userId", userId.toString());
                user.put("username", db.getUserName(userId));
                users.add(user);
            }

            return users;
        } catch (SQLException e) {
            throw new ServerErrorException(e);
        } finally {
            db.close(stmt, rs);
        }
    }
}
