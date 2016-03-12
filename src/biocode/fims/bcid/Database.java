package biocode.fims.bcid;

import biocode.fims.fimsExceptions.ServerErrorException;
import biocode.fims.settings.*;
import org.apache.commons.dbcp2.BasicDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.sql.*;
import java.sql.Connection;

/**
 * Creates the connection for the backend Bcid Database.
 * Settings come from the biocode.fims.settings.SettingsManager/Property file defining the user/password/url/class
 * for the mysql Database where the data lives.
 */
public class Database {

    private static DataSource bcidDataSource;
    private static String bcidUser;
    private static String bcidPassword;
    private static String bcidUrl;
    private static String bcidClass;

    final static Logger logger = LoggerFactory.getLogger(Database.class);

    static {
        SettingsManager sm = SettingsManager.getInstance();
        bcidUser = sm.retrieveValue("bcidUser");
        bcidPassword = sm.retrieveValue("bcidPassword");
        bcidUrl = sm.retrieveValue("bcidUrl");
        bcidClass = sm.retrieveValue("bcidClass");
    }

    /**
     * Load settings for creating this Database connection from the bcidsettings.properties file
     */
    public Database() {
        // Construct BasicDataSource
        BasicDataSource bds = new BasicDataSource();
        bds.setDriverClassName(bcidClass);
        bds.setUrl(bcidUrl);
        bds.setUsername(bcidUser);
        bds.setPassword(bcidPassword);

        bcidDataSource = bds;

    }

    public Connection getBcidConn() {
        try {
            return bcidDataSource.getConnection();
        } catch (SQLException e) {
            throw new ServerErrorException(e);
        }
    }

    public void close(Connection conn, Statement stmt, ResultSet rs) {
        if (stmt != null) {
            try {
                stmt.close();
            } catch (SQLException e) {
                logger.warn("SQLException while attempting to close PreparedStatement.", e);
            }
        }
        if (rs != null) {
            try {
                rs.close();
            } catch (SQLException e) {
                logger.warn("SQLException while attempting to close ResultSet.", e);
            }
        }
        if (conn != null) {
            try {
                conn.close();
            } catch (SQLException e) {
                logger.warn("SQLException while attempting to close Connection.", e);
            }
        }
        return;
    }


    public void close(Connection conn, PreparedStatement stmt, ResultSet rs) {
       close(conn, (Statement)stmt, rs);
    }

    /**
     * Return the userID given a username
     * @param username
     * @return
     */
    public Integer getUserId(String username) {
        PreparedStatement stmt = null;
        ResultSet rs = null;
        Connection conn = getBcidConn();
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
        Connection conn = getBcidConn();
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
