package biocode.fims.bcid;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;

/**
 * abstract class to hold basic Database methods
 */
public abstract class Database {
    final static Logger logger = LoggerFactory.getLogger(Database.class);

    public static void close(Connection conn, Statement stmt, ResultSet rs) {
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


    public static void close(Connection conn, PreparedStatement stmt, ResultSet rs) {
        close(conn, (Statement)stmt, rs);
    }
}
