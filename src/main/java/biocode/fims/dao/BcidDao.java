package biocode.fims.dao;

import biocode.fims.bcid.BcidDatabase;
import biocode.fims.entities.Bcid;
import biocode.fims.fimsExceptions.ServerErrorException;

import java.math.BigInteger;
import java.net.URI;
import java.net.URISyntaxException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;

import static com.sun.org.apache.xml.internal.security.utils.Base64.encode;

/**
 * Created by rjewing on 4/8/16.
 */
public class BcidDao {

    public void update(Bcid bcid) {}

    public int create(Bcid bcid) {
        // Insert the values into the Database
        PreparedStatement insertStatement = null;
        PreparedStatement updateStatement = null;
        Connection conn = BcidDatabase.getConnection();
        try {
            // Use auto increment in Database to assign the actual Bcid.. this is threadsafe this way
            String insertString = "INSERT INTO bcids (userId, resourceType, doi, webAddress, graph, title, internalId, ezidRequest, suffixPassThrough, finalCopy) " +
                    "values (?,?,?,?,?,?,?,?,?,?)";

            insertStatement = conn.prepareStatement(insertString);
            insertStatement.setInt(1, bcid.getUserId());
            insertStatement.setString(2, bcid.getResourceType());
            insertStatement.setString(3, bcid.getDoi());
            if (bcid.getWebAddress() != null)
                insertStatement.setString(4, bcid.getWebAddress().toString());
            else
                insertStatement.setString(4, null);
            insertStatement.setString(5, bcid.getGraph());
            insertStatement.setString(6, bcid.getTitle());
            insertStatement.setString(7, bcid.getInternalId().toString());
            insertStatement.setBoolean(8, bcid.isEzidRequest());
            insertStatement.setBoolean(9, bcid.isSuffixPassThrough());
            insertStatement.setBoolean(10, bcid.isFinalCopy());

            int affectedRows = insertStatement.executeUpdate();

            if (affectedRows == 0) {
                throw new SQLException("Creating user failed, no rows affected.");
            }

            try (ResultSet generatedKeys = insertStatement.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    return (int) generatedKeys.getLong(1);
                }
                else {
                    throw new SQLException("Creating user failed, no ID obtained.");
                }
            }


        } catch (SQLException e) {
            throw new ServerErrorException("Server Error", "SQLException while creating a bcid for user: " + bcid.getUserId(), e);
        } finally {
            BcidDatabase.close(null, insertStatement, null);
            BcidDatabase.close(conn, updateStatement, null);
        }
    }


}
