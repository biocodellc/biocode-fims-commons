package biocode.fims.bcid;

import biocode.fims.fimsExceptions.ServerErrorException;
import biocode.fims.settings.*;
import org.apache.commons.dbcp2.BasicDataSource;

import java.sql.*;
import java.sql.Connection;

/**
 * Creates the connection for the backend Bcid Database.
 * Settings come from the biocode.fims.settings.SettingsManager/Property file defining the user/password/url/class
 * for the mysql Database where the data lives.
 */
public final class BcidDatabase extends Database {

    private final static BasicDataSource bcidDataSource = new BasicDataSource();

    static {
        SettingsManager sm = SettingsManager.getInstance();
        bcidDataSource.setUsername(sm.retrieveValue("bcidUser"));
        bcidDataSource.setPassword(sm.retrieveValue("bcidPassword"));
        bcidDataSource.setUrl(sm.retrieveValue("bcidUrl"));
        bcidDataSource.setDriverClassName(sm.retrieveValue("bcidClass"));
    }

    private BcidDatabase() {}

    public static Connection getConnection() {
        try {
            return bcidDataSource.getConnection();
        } catch (SQLException e) {
            throw new ServerErrorException(e);
        }
    }

    /**
     * Return the userID given a username
     * @param username
     * @return
     */
    public static Integer getUserId(String username) {
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
    public static String getUserName(Integer userId) {
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
