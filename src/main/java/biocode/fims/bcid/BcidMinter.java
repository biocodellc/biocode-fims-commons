package biocode.fims.bcid;

import biocode.fims.ezid.EzidException;
import biocode.fims.ezid.EzidService;
import biocode.fims.fimsExceptions.BadRequestException;
import biocode.fims.fimsExceptions.ServerErrorException;
import biocode.fims.settings.SettingsManager;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigInteger;
import java.net.URI;
import java.net.URISyntaxException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.UUID;

/**
 * This class mints shoulders for use in the  EZID systems known as data groups.
 * Minting data groups are important in establishing the ownership of particular data
 * elements.
 */
public class BcidMinter extends BcidEncoder {

    final static Logger logger = LoggerFactory.getLogger(BcidMinter.class);
    private Boolean ezidRequest;

    private static SettingsManager sm;

    protected String bow = "";
    protected String scheme = "ark:";
    protected String shoulder = "";

    static {
        sm = SettingsManager.getInstance();
    }

    /**
     * Default to ezidRequest = false using default Constructor
     */
    public BcidMinter() {
        this(false);
    }

    /**
     * Default constructor for data group uses the temporary identifier ark:/99999/fk4.  Values can be overridden in
     * the mint method.
     */
    public BcidMinter(boolean ezidRequest) {
        bcidMinterSetup(null, 99999);
        this.ezidRequest = ezidRequest;
    }

    /**
     * general BcidMinter setup used by the constructors
     */
    private void bcidMinterSetup (String shoulder, Integer NAAN) {
        // Generate defaults in constructor, these will be overridden later
        if (shoulder == null) {
            this.shoulder = "fk4";
        } else {
            this.shoulder = shoulder;
        }
        setBow(NAAN);
        try {
            URI identifier = new URI(bow + this.shoulder);
            checkBcidExists(identifier.toString());
        } catch (URISyntaxException e) {
            throw new ServerErrorException("Server Error", bow + this.shoulder + " is not a valid URI", e);
        }
    }

    /**
     * Get the projectCode given a bcidId
     *
     * @param bcidId
     */
    public String getProject(Integer bcidId) {
        String projectCode = "";
        String sql = "select p.projectCode from projects p, expeditionBcids eb, expeditions e, " +
                "bcids b where b.bcidId = eb.bcidId and e.expeditionId=eb.`expeditionId` " +
                "and e.`projectId`=p.`projectId` and b.bcidId= ?";


        PreparedStatement stmt = null;
        ResultSet rs = null;
        Connection conn = BcidDatabase.getConnection();
        try {
            stmt = conn.prepareStatement(sql);
            stmt.setInt(1, bcidId);
            rs = stmt.executeQuery();
            if (rs.isLast())
            if (rs.next()) {
                projectCode = rs.getString("projectCode");
            }
        } catch (SQLException e) {
            throw new ServerErrorException("Server Error",
                    "Exception retrieving projectCode for bcidId: " + bcidId, e);
        } finally {
            BcidDatabase.close(conn, stmt, rs);
        }

        return projectCode;
    }

    /**
     * Set the bow using this method always
     *
     * @param naan
     */
    private void setBow(Integer naan) {
        this.bow = scheme + "/" + naan + "/";
    }

    /**
     * Mint a Bcid, providing a Bcid to insert into Database
     *
     * @param NAAN
     */
    private String mint(Integer NAAN, Bcid bcid) {

        URI identifier;
        // Never request EZID for user=demo
        if (BcidDatabase.getUserName(bcid.userId).equalsIgnoreCase("demo")) {
            ezidRequest = false;
        }
        this.bow = scheme + "/" + NAAN + "/";

        // Generate an internal ID to track this submission
        UUID internalId = UUID.randomUUID();

        // Insert the values into the Database
        PreparedStatement insertStatement = null;
        PreparedStatement updateStatement = null;
        Connection conn = BcidDatabase.getConnection();
        try {
            // Use auto increment in Database to assign the actual Bcid.. this is threadsafe this way
            String insertString = "INSERT INTO bcids (userId, resourceType, doi, webAddress, graph, title, internalId, ezidRequest, finalCopy) " +
                    "values (?,?,?,?,?,?,?,?,?,?)";

            insertStatement = conn.prepareStatement(insertString);
            insertStatement.setInt(1, bcid.userId);
            insertStatement.setString(2, bcid.resourceType);
            insertStatement.setString(3, bcid.doi);
            if (bcid.webAddress != null)
                insertStatement.setString(4, bcid.webAddress.toString());
             else
                insertStatement.setString(4, null);
            insertStatement.setString(5, bcid.graph);
            insertStatement.setString(6, bcid.title);
            insertStatement.setString(7, internalId.toString());
            insertStatement.setBoolean(8, ezidRequest);
            insertStatement.setBoolean(9, bcid.finalCopy);
            insertStatement.execute();

            // Get the bcidId that was assigned
            Integer bcidId = checkBcidExists(internalId);

            // Create the shoulder Bcid (String Bcid Bcid)
            shoulder = encode(new BigInteger(bcidId.toString()));

            // Create the identifier
            identifier = new URI(bow + shoulder);

            // Update the shoulder, and hence identifier, now that we know the bcidId
            String updateString = "UPDATE bcids" +
                    " SET identifier = ?" +
                    " WHERE bcidId = ?";
            updateStatement = conn.prepareStatement(updateString);

            updateStatement.setString(1, identifier.toString());
            updateStatement.setInt(2, bcidId);

            updateStatement.executeUpdate();

        } catch (SQLException e) {
            throw new ServerErrorException("Server Error", "SQLException while creating a bcid for user: " + bcid.userId, e);
        } catch (URISyntaxException e) {
            throw new ServerErrorException("Server Error", bow + shoulder + " is not a valid URI", e);
        } finally {
            BcidDatabase.close(null, insertStatement, null);
            BcidDatabase.close(conn, updateStatement, null);
        }

        return identifier.toString();
    }

    /**
     * Return the bcidId given the internalId
     *
     * @param bcidUUID
     *
     * @return
     *
     * @throws SQLException
     */
    private Integer checkBcidExists(UUID bcidUUID) throws SQLException {
        PreparedStatement stmt = null;
        ResultSet rs = null;
        Connection conn = BcidDatabase.getConnection();

        try {
            String sql = "select bcidId from bcids where internalId = ?";
            stmt = conn.prepareStatement(sql);
            stmt.setString(1, bcidUUID.toString());
            rs = stmt.executeQuery();
            rs.next();
            return rs.getInt("bcidId");
        } finally {
            BcidDatabase.close(conn, stmt, rs);
        }
    }

    /**
     * Check to see if a Bcid exists or not
     *
     * @param identifier
     */
    public void checkBcidExists(String identifier) {
        PreparedStatement stmt = null;
        ResultSet rs = null;
        Connection conn = BcidDatabase.getConnection();

        try {
            String sql = "select bcidId from bcids where identifier = ?";
            stmt = conn.prepareStatement(sql);
            stmt.setString(1, identifier);
            rs = stmt.executeQuery();
            rs.next();
        } catch (SQLException e) {
            throw new ServerErrorException("Server Error",
                    "Exception retrieving bcidId for bcid with identifier: " + identifier, e);
        } finally {
            BcidDatabase.close(conn, stmt, rs);
        }
        return;
    }

    /**
     * Return a JSON representation of a bcidList
     *
     * @param username
     *
     * @return
     */
    public JSONArray bcidList(String username) {
        JSONArray bcids = new JSONArray();

        PreparedStatement stmt = null;
        ResultSet rs = null;
        Connection conn = BcidDatabase.getConnection();

        try {
            String sql = "select b.bcidId as bcidId, concat_ws(' ',identifier,title) as identifier from bcids b, users u where u.username = ? && " +
                    "b.userId=u.userId";
            stmt = conn.prepareStatement(sql);

            stmt.setString(1, username);
            rs = stmt.executeQuery();
            while (rs.next()) {
                JSONObject bcid = new JSONObject();
                bcid.put("bcidId", rs.getInt("bcidId"));
                bcid.put("identifier", rs.getString("identifier"));
                bcids.add(bcid);
            }

        } catch (SQLException e) {
            throw new ServerErrorException("Server Error", "Exception retrieving bcids for user " + username, e);
        } finally {
            BcidDatabase.close(conn, stmt, rs);
        }
        return bcids;
    }

    /**
     * return a BCID formatted with LINK
     *
     * @param pPrefix
     *
     * @return
     */
    public String getEZIDLink(String pPrefix, String username, String linkText) {
        if (!username.equals("demo")) {
            return "(<a href='http://n2t.net/" + pPrefix + "'>" + linkText + "</a>)";
        } else {
            return "";
        }
    }

    /**
     * return a BCID formatted with LINK
     *
     * @param pPrefix
     *
     * @return
     */
    public String getEZIDMetadataLink(String pPrefix, String username, String linkText) {
        if (!username.equals("demo")) {
            return "(<a href='http://ezid.cdlib.org/id/" + pPrefix + "'>" + linkText + "</a>)";
        } else {
            return "";
            //return "(<a href='" + resolverTargetPrefix + pPrefix + "'>metadata</a>)";
        }
    }

    /**
     * return a DOI formatted with LINK
     *
     * @param pDOI
     *
     * @return
     */
    public String getDOILink(String pDOI) {
        if (pDOI != null && !pDOI.trim().equals("")) {
            return "<a href='http://dx.doi.org/" + pDOI + "'>http://dx.doi.org/" + pDOI + "</a>";
        } else {
            return "";
        }
    }

    /**
     * Return a Metadata link for DOI
     *
     * @param pDOI
     *
     * @return
     */
    public String getDOIMetadataLink(String pDOI) {
        if (pDOI != null && !pDOI.trim().equals("")) {
            return "(<a href='http://data.datacite.org/text/html/" + pDOI.replace("doi:", "") + "'>metadata</a>)";
        } else {
            return "";
        }
    }

    public static void main(String args[]) {
        BcidMinter b = new BcidMinter();
        try {
//            System.out.println(b.bcidTable("biocode"));
        } catch (Exception e) {
            e.printStackTrace();
        }


    }

    /**
     * fetch a BCID's metadata given an identifier
     *
     * @param identifier
     *
     * @return
     */
    public Hashtable<String, String> getBcidMetadata(String identifier) {
        Hashtable<String, String> metadata = new Hashtable<String, String>();
        ResourceTypes rts = new ResourceTypes();

        PreparedStatement stmt = null;
        ResultSet rs = null;
        Connection conn = BcidDatabase.getConnection();

        try {
            String sql = "SELECT doi, title, webAddress, resourceType " +
                    "FROM bcids WHERE BINARY identifier = ?";
            stmt = conn.prepareStatement(sql);

            stmt.setString(1, identifier);

            rs = stmt.executeQuery();
            if (rs.next()) {
                if (rs.getString("doi") != null) {
                    metadata.put("doi", rs.getString("doi"));
                }
                if (rs.getString("title") != null) {
                    metadata.put("title", rs.getString("title"));
                }
                if (rs.getString("webAddress") != null) {
                    metadata.put("webAddress", rs.getString("webAddress"));
                }

                ResourceType resourceType = rts.get(rs.getString("resourceType"));
                if (resourceType != null) {
                    metadata.put("resourceType", resourceType.string);
                } else {
                    metadata.put("resourceType", rs.getString("resourceType"));
                }

            } else {
                throw new BadRequestException("BCIDs not found. Are you the owner of this bcid?");
            }
        } catch (SQLException e) {
            throw new ServerErrorException("Server Error", "SQLException while retrieving configuration for " +
                    "bcid with identifier: " + identifier, e);
        } finally {
            BcidDatabase.close(conn, stmt, rs);
        }
        return metadata;
    }

    /**
     * verify that an indentifier exists and the user owns the bcid
     * @param identifier
     * @param userId
     * @return
     */
    public Boolean userOwnsBcid(String identifier, Integer userId) {
        PreparedStatement stmt = null;
        ResultSet rs = null;
        Connection conn = BcidDatabase.getConnection();

        try {
            String sql = "SELECT count(*) as count FROM bcids WHERE BINARY identifier = ? AND userId = ?";
            stmt = conn.prepareStatement(sql);

            stmt.setString(1, identifier);
            stmt.setInt(2, userId);

            rs = stmt.executeQuery();
            rs.next();

            return rs.getInt("count") > 0;
        } catch (SQLException e) {
            throw new ServerErrorException(e);
        } finally {
            BcidDatabase.close(conn, stmt, rs);
        }
    }

    /**
     * update a Bcid's metadata
     *
     * @param metadata a Hashtable<String, String> which has the bcids table fields to be updated as key, new value
     *               pairs
     * @param identifier the ark:// for the BICD
     *
     * @return
     */
    public Boolean updateBcidMetadata(Hashtable<String, String> metadata, String identifier) {
        PreparedStatement stmt = null;
        Connection conn = BcidDatabase.getConnection();
        try {
            String sql = "UPDATE bcids SET ";

            // update resourceTypeString to the correct uri
            if (metadata.containsKey("resourceTypeString")) {
                metadata.put("resourceType", new ResourceTypes().getByName(metadata.get("resourceTypeString")).uri);
                metadata.remove("resourceTypeString");
            }

            // Dynamically create our UPDATE statement depending on which fields the user wants to update
            for (Enumeration e = metadata.keys(); e.hasMoreElements(); ) {
                String key = e.nextElement().toString();
                sql += key + " = ?";

                if (e.hasMoreElements()) {
                    sql += ", ";
                } else {
                    sql += " WHERE BINARY identifier =\"" + identifier + "\";";
                }
            }

            stmt = conn.prepareStatement(sql);

            // place the parametrized values into the SQL statement
            {
                int i = 1;
                for (Enumeration e = metadata.keys(); e.hasMoreElements(); ) {
                    String key = e.nextElement().toString();
                    if (key.equals("suffixPassthrough")) {
                        if (metadata.get(key).equalsIgnoreCase("true")) {
                            stmt.setBoolean(i, true);
                        } else {
                            stmt.setBoolean(i, false);
                        }
                    } else if (metadata.get(key).equals("")) {
                        stmt.setString(i, null);
                    } else {
                        stmt.setString(i, metadata.get(key));
                    }
                    i++;
                }
            }

            Integer result = stmt.executeUpdate();
            // result should be '1', if not, nothing was updated
            if (result >= 1) {
                return true;
            } else {
                // if here, then nothing was updated due to the Bcid not being found
                return false;
            }
        } catch (SQLException e) {
            throw new ServerErrorException("Server Error", "SQLException while updating configuration for " +
                    "bcid with identifier: " + identifier, e);
        } finally {
            BcidDatabase.close(conn, stmt, null);
        }
    }

    /**
     * create Bcid's corresponding to expeditions
     */
    public String createEntityBcid(Bcid bcid) {

        String identifier = mint(
                new Integer(sm.retrieveValue("naan")),
                bcid
        );

        // Create EZIDs right away for Bcid level Identifiers
        // Initialize ezid account
        // NOTE: On any type of EZID error, we DON'T want to fail the process.. This means we need
        // a separate mechanism on the server side to check creation of EZIDs.  This is easy enough to do
        // in the Database.

        if (ezidRequest) {
            ManageEZID creator = new ManageEZID();
            try {
                EzidService ezidAccount = new EzidService();
                // Setup EZID account/login information
                ezidAccount.login(sm.retrieveValue("eziduser"), sm.retrieveValue("ezidpass"));
                creator.createBcidsEZIDs(ezidAccount);
            } catch (EzidException e) {
                logger.warn("EZID NOT CREATED FOR BCID = " + identifier, e);
            } finally {
            }
        }
        return identifier;
    }

    /**
     * update the bcid ts to the current time
     * @param graph the graph of the bcid
     */
    public void updateBcidTimestamp(String graph) {
        PreparedStatement stmt = null;
        Connection conn = BcidDatabase.getConnection();
        try {
            String sql = "UPDATE bcids SET ts=now() WHERE graph=?";
            stmt = conn.prepareStatement(sql);

            stmt.setString(1, graph);

            stmt.execute();
        } catch (SQLException e) {
            throw new ServerErrorException(e);
        } finally {
            Database.close(conn, stmt, null);
        }
    }
}