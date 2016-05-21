package biocode.fims.bcid;

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

/**
 * Class to manage user creation and profile information.
 */
public class UserMinter {

    public UserMinter() {
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
        Connection conn = BcidDatabase.getConnection();
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
            BcidDatabase.close(conn, stmt, rs);
        }

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
        Connection conn = BcidDatabase.getConnection();

        String username = p.validateToken(token);
        if (username != null) {
            Integer userId = BcidDatabase.getUserId(username);
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

                    return profile;
                }
            } catch (SQLException e) {
                throw new ServerErrorException(e);
            } finally {
                BcidDatabase.close(conn, stmt, rs);
            }
        }

        throw new BadRequestException("invalid_grant", "access token is not valid");
    }

    /**
     * retrieve all users
     * @return
     */
    public JSONArray getUsers() {
        JSONArray users = new JSONArray();
        PreparedStatement stmt = null;
        ResultSet rs = null;
        Connection conn = BcidDatabase.getConnection();
        try {
            String sql = "SELECT userId FROM users";
            stmt = conn.prepareStatement(sql);

            rs = stmt.executeQuery();

            while (rs.next()) {
                JSONObject user = new JSONObject();
                Integer userId = rs.getInt("userId");
                user.put("userId", userId.toString());
                user.put("username", BcidDatabase.getUserName(userId));
                users.add(user);
            }

            return users;
        } catch (SQLException e) {
            throw new ServerErrorException(e);
        } finally {
            BcidDatabase.close(conn, stmt, rs);
        }
    }
}
