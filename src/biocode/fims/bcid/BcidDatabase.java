package biocode.fims.bcid;

import biocode.fims.fimsExceptions.ServerErrorException;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;
import java.sql.*;
import java.sql.Connection;

/**
 * Creates the connection for the backend Bcid Database.
 * Settings come from the biocode.fims.settings.SettingsManager/Property file defining the user/password/url/class
 * for the mysql Database where the data lives.
 */
public final class BcidDatabase extends Database {

    public BcidDatabase() {
        try {
            InitialContext ctx = new InitialContext();
            dataSource = (DataSource) ctx.lookup("java:comp/env/jdbc/bcid");
        } catch (NamingException e) {
            throw new ServerErrorException("Error connecting to Biscicol db");
        }
    }

    /**
     * Return the userID given a username
     * @param username
     * @return
     */
    public Integer getUserId(String username) {
        PreparedStatement stmt = null;
        ResultSet rs = null;
        Connection conn = getConnection();
        try {
            String sql = "Select userId from users where username=?";
            stmt = conn.prepareStatement(sql);
            stmt.setString(1, username);

            rs = stmt.executeQuery();

            if (rs.next()) {
                return rs.getInt("userId");
            }
        } catch (SQLException e) {
            throw new ServerErrorException("Server Error",
                    "SQLException attempting to getUserId when given the username: {}", e);
        } finally {
            close(conn, stmt, rs);
        }
        return null;
    }
    /**
     * Return the username given a userId
     * @param userId
     * @return
     */
    public String getUserName(Integer userId) {
        PreparedStatement stmt = null;
        ResultSet rs = null;
        Connection conn = getConnection();
        try {
            String sql = "SELECT username FROM users WHERE userId = ?";
            stmt = conn.prepareStatement(sql);
            stmt.setInt(1, userId);

            rs = stmt.executeQuery();

            if (rs.next()) {
                return rs.getString("username");
            }
        } catch (SQLException e) {
            throw new ServerErrorException("Server Error",
                    "SQLException attempting to getUserName when given the userId: {}", e);
        } finally {
            close(conn, stmt, rs);
        }
        return null;
    }

}
