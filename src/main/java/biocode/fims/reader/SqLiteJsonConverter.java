package biocode.fims.reader;

import biocode.fims.bcid.Database;
import biocode.fims.digester.Attribute;
import biocode.fims.digester.Entity;
import biocode.fims.digester.Mapping;
import biocode.fims.fimsExceptions.FimsRuntimeException;
import biocode.fims.fimsExceptions.errorCodes.ValidationCode;
import biocode.fims.settings.Hasher;
import biocode.fims.utils.SqlLiteNameCleaner;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.*;


/**
 * Takes a data source represented by a JsonArray and converts it to a
 * SQLite Database.
 */
public final class SqLiteJsonConverter {
    private final JSONArray dataset;
    String dest;

    private static Logger logger = LoggerFactory.getLogger(SqLiteJsonConverter.class);

    /**
     * @param dataset
     * @param dest    A valid SQLIte JDBC connection string.
     */
    public SqLiteJsonConverter(JSONArray dataset, String dest) {
        this.dataset = dataset;
        this.dest = dest;
    }

    /**
     * Ensures that table and column names are valid SQLite identifiers that do
     * not require quoting in brackets for maximum compatibility.  Spaces and
     * periods are replaced with underscores, and if the name starts with a
     * digit, an underscore is added to the beginning of the name.  Any other
     * non-alphanumeric characters are removed.
     *
     * @param tName The table name to fix, if needed.
     * @return The corrected table name.
     */
    private String fixSQLiteIdentifierName(String tName) {
        SqlLiteNameCleaner cleaner = new SqlLiteNameCleaner();
        return cleaner.fixNames(tName);
    }

    /**
     * Builds hashes from source data.  This should be called AFTER the convert method
     * below as we want helpful Rules messages displayed before we attempt this method.
     * We don't want the WHOLE connection to fail as the hash methods are more brittle than
     * other items in the convert process.
     *
     * @param mapping
     */
    public void buildHashes(Mapping mapping, String tableName) {
        Connection connection = null;
        try {
            connection = DriverManager.getConnection(dest);
            String fixedtName = fixSQLiteIdentifierName(tableName);
            buildHashes(connection, mapping, fixedtName);
        } catch (SQLException e) {
            // If we through an exception here, it could be ANYTHING that SQLlite doesn't like, including duplicate columnnames
            throw new FimsRuntimeException(500,e);
        } finally {
            try {
                if (connection != null)
                    connection.close();
            } catch (SQLException e) {
                logger.warn("SQLException", e);
            }
        }
    }

    /**
     * Reads the source data and converts it to a Sqlite Database.
     * Uses the Database connection string provided in the constructor
     * Any tables that already exist in the destination Database will
     * be DROPPED.  The table will have columns matching the keys of the first resource
     */
    public void convert(String tableName) {
        Connection connection = null;
        try {
            connection = DriverManager.getConnection(dest);
            String fixedtName = fixSQLiteIdentifierName(tableName);

            buildTable(connection, fixedtName);
        } catch (SQLException e) {
            // If we through an exception here, it could be ANYTHING that SQLlite doesn't like, including duplicate column names
            throw new FimsRuntimeException(500, e);
        } finally {
            try {
                if (connection != null)
                    connection.close();
            } catch (SQLException e) {
                logger.warn("SQLException", e);
            }
        }
    }

    /**
     * build Hash Keys in SQLLite table when we encounter a worksheetUniqueKey that ends in HASH
     *
     * @param connection
     * @param mapping
     */
    private void buildHashes(Connection connection, Mapping mapping, String tName)  {
        // Loop through entities and find which ones define HASH
        Statement stmt = null;
        try {
            stmt = connection.createStatement();

            for (Entity entity: mapping.getEntities()) {
                if (entity.hasWorksheet() && entity.getUniqueKey().contains("HASH")) {

                    // Add this column Bcid
                    String alter = "ALTER TABLE " + tName + " ADD COLUMN " + entity.getUniqueKey() + " text";
                    stmt.executeUpdate(alter);

                    LinkedList<Attribute> attributes = entity.getAttributes();
                    Iterator attributesIt = attributes.iterator();
                    StringBuilder sb = new StringBuilder();
                    sb.append("SELECT rowid,");
                    while (attributesIt.hasNext()) {
                        Attribute attribute = (Attribute) attributesIt.next();
                        sb.append(attribute.getColumn());
                        if (attributesIt.hasNext())
                            sb.append(" || ");
                    }
                    sb.append(" AS toHash FROM " + tName);
                    System.out.println(sb.toString());

                    ResultSet rs = stmt.executeQuery(sb.toString());

                    Statement updateStatement = connection.createStatement();
                    Hasher hasher = new Hasher();
                    updateStatement.execute("BEGIN TRANSACTION");
                    while (rs.next()) {
                        String update = "UPDATE " + tName +
                                " SET " + entity.getUniqueKey() + " = \"" +
                                hasher.hasherDigester(rs.getString("toHash")) + "\" " +
                                " WHERE rowid = " + rs.getString("rowid");
                        updateStatement.executeUpdate(update);
                    }
                    updateStatement.execute("COMMIT");
                    updateStatement.close();
                }
            }
        } catch (SQLException e) {
            //throw new SQLException(e);
            throw new FimsRuntimeException(500, e);
        } finally {
            try {
                stmt.close();
            } catch (SQLException e) {
                logger.warn("SQLException", e);
            }
        }
    }

    /**
     * Creates a single table in the destination Database. If the specified table name
     * already exists in the Database, IT IS DROPPED.  A new table with columns matching the keys
     * of the first resource in the dataset and all resources from the dataset
     * are copied to the new table.
     *
     * @param conn  A valid connection to a destination Database.
     * @param tName The name to use for the table in the destination Database.
     */
    private void buildTable(Connection conn, String tName) {
        if (dataset.isEmpty()) {
            throw new FimsRuntimeException(ValidationCode.EMPTY_DATASET, 400);
        }

        int colcnt, cnt;
        Statement stmt = null;
        PreparedStatement insstmt = null;
        try {
            stmt = conn.createStatement();
            // Counter for machine-generated column names.
            int col_cnt = 0;

            // Generate a short string of random characters to use for machine-
            // generated column names if the data source provides a blank column
            // name.
            char[] rand_prefix_arr = new char[10];
            String alphabet = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
            int alphindex;
            Random randgen = new Random();
            for (cnt = 0; cnt < rand_prefix_arr.length; cnt++) {
                alphindex = randgen.nextInt(alphabet.length());
                rand_prefix_arr[cnt] = alphabet.charAt(alphindex);
            }
            String randPrefix = String.copyValueOf(rand_prefix_arr);

            // if this table exists, drop it
            stmt.executeUpdate("DROP TABLE IF EXISTS [" + tName + "]");

            // set up the table definition query
            String query = "CREATE TABLE [" + tName + "] (";
            colcnt = 0;
            List<String> columns = new ArrayList<>(((JSONObject) dataset.get(0)).keySet());
            for (String colname : columns) {
                if (colcnt++ > 0) {
                    query += ", ";
                }

                // If the column name is blank, generate a suitable name.
                if (colname.trim().equals("")) {
                    colname = tName + "_" + randPrefix + "_" + col_cnt;
                    col_cnt++;
                }
                colname = fixSQLiteIdentifierName(colname);
                query += "\"" + colname + "\"";
            }
            query += ")";

            // create the table
            stmt.executeUpdate(query);

            // create a prepared statement for insert queries
            query = "INSERT INTO [" + tName + "] VALUES (";
            for (cnt = 0; cnt < colcnt; cnt++) {
                if (cnt > 0)
                    query += ", ";
                query += "?";
            }
            query += ")";
            insstmt = conn.prepareStatement(query);

            // Start a new transaction for all of the INSERT statements.  This
            // dramatically improves the run time from many minutes for a large data
            // source to a matter of seconds.
            stmt.execute("BEGIN TRANSACTION");

            // populate the table with the source data
            for (Object obj : dataset) {
                JSONObject resource = (JSONObject) obj;
                cnt = 0;
                StringBuilder sb = new StringBuilder();
                for (String col : columns) {
                    String dataval = String.valueOf(resource.get(col));
                    // Ignore any data that is NA or na by setting it to blank
                    if (dataval.equalsIgnoreCase("na"))
                        dataval = "";
                    sb.append(dataval);
                    insstmt.setString(++cnt, dataval);
                }

                // Supply blank strings for any missing columns.  This does not appear
                // to be strictly necessary, at least with the Sqlite driver we're
                // using, but it is included as insurance against future changes.
                while (cnt < colcnt) {
                    insstmt.setString(++cnt, "");
                }

                // Only execute this portion of the strinbuilder identified ANY datavalues for this row
                if (!sb.toString().equals("")) {
                    // add the row to the Database
                    insstmt.executeUpdate();
                }
            }

            // end the transaction
            stmt.execute("COMMIT");
        } catch (SQLException e) {
            String msg = "Unable to parse spreadsheet.";
            throw new FimsRuntimeException(msg, null, 500, e);
        } finally {
            Database.close(null, stmt, null);
            Database.close(null, insstmt, null);
        }
    }
}
