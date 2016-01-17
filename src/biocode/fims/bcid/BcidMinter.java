package biocode.fims.bcid;

import biocode.fims.ezid.EzidException;
import biocode.fims.ezid.EzidService;
import biocode.fims.fimsExceptions.BadRequestException;
import biocode.fims.fimsExceptions.ServerErrorException;
import biocode.fims.settings.SettingsManager;
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

    // Mysql Connection
    protected Connection conn;
    Database db;
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
        db = new Database();
        conn = db.getConn();
        // Generate defaults in constructor, these will be overridden later
        if (shoulder == null) {
            this.shoulder = "fk4";
        } else {
            this.shoulder = shoulder;
        }
        setBow(NAAN);
        try {
            URI identifier = new URI(bow + this.shoulder);
            getBcidsId(identifier.toString());
        } catch (URISyntaxException e) {
            throw new ServerErrorException("Server Error", bow + this.shoulder + " is not a valid URI", e);
        }
    }

    /**
     * Get the projectCode given a bcidsId
     *
     * @param bcidId
     */
    public String getProject(Integer bcidId) {
        String projectCode = "";
        String sql = "select p.projectCode from projects p, expeditionBcids eb, expeditions e, " +
                "bcids b where b.bcidId = eb.bcidId and e.expeditionId=eb.`expeditionId` " +
                "and e.`projectId`=p.`projectId` and b.bcidId= ?";

        System.out.println("sql = " + sql + "    bcidsId = " + bcidId);

        PreparedStatement stmt = null;
        ResultSet rs = null;
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
                    "Exception retrieving projectCode for bcidsId: " + bcidId, e);
        } finally {
            db.close(stmt, rs);
        }

        System.out.println(projectCode);
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
        if (db.getUserName(bcid.userId).equalsIgnoreCase("demo")) {
            ezidRequest = false;
        }
        this.bow = scheme + "/" + NAAN + "/";

        // Generate an internal ID to track this submission
        UUID internalId = UUID.randomUUID();

        // Insert the values into the Database
        PreparedStatement insertStatement = null;
        PreparedStatement updateStatement = null;
        try {
            // Use auto increment in Database to assign the actual Bcid.. this is threadsafe this way
            String insertString = "INSERT INTO bcids (userId, resourceType, doi, webAddress, graph, title, internalId, ezidRequest, suffixPassThrough, finalCopy) " +
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
            insertStatement.setBoolean(9, bcid.suffixPassThrough);
            insertStatement.setBoolean(10, bcid.finalCopy);
            insertStatement.execute();

            // Get the bcidsId that was assigned
            Integer bcidsId = getBcidsId(internalId);

            // Create the shoulder Bcid (String Bcid Bcid)
            shoulder = encode(new BigInteger(bcidsId.toString()));

            // Create the identifier
            identifier = new URI(bow + shoulder);

            // Update the shoulder, and hence identifier, now that we know the bcidsId
            String updateString = "UPDATE bcids" +
                    " SET identifier = ?" +
                    " WHERE bcidId = ?";
            updateStatement = conn.prepareStatement(updateString);

            updateStatement.setString(1, identifier.toString());
            updateStatement.setInt(2, bcidsId);

            updateStatement.executeUpdate();

        } catch (SQLException e) {
            throw new ServerErrorException("Server Error", "SQLException while creating a bcid for user: " + bcid.userId, e);
        } catch (URISyntaxException e) {
            throw new ServerErrorException("Server Error", bow + shoulder + " is not a valid URI", e);
        } finally {
            db.close(insertStatement, null);
            db.close(updateStatement, null);
        }

        return identifier.toString();
    }

    /**
     * Return the bcidsId given the internalId
     *
     * @param bcidUUID
     *
     * @return
     *
     * @throws SQLException
     */
    private Integer getBcidsId(UUID bcidUUID) throws SQLException {
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            String sql = "select bcidId from bcids where internalId = ?";
            stmt = conn.prepareStatement(sql);
            stmt.setString(1, bcidUUID.toString());
            rs = stmt.executeQuery();
            rs.next();
            return rs.getInt("bcidId");
        } finally {
            db.close(stmt, rs);
        }
    }

    /**
     * Check to see if a Bcid exists or not
     *
     * @param identifier
     *
     * @return An Integer representing a Bcid
     */
    public Integer getBcidsId(String identifier) {
        Integer bcidsId = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            String sql = "select bcidId from bcids where identifier = ?";
            stmt = conn.prepareStatement(sql);
            stmt.setString(1, identifier);
            rs = stmt.executeQuery();
            rs.next();
            bcidsId = rs.getInt("bcidId");
        } catch (SQLException e) {
            throw new ServerErrorException("Server Error",
                    "Exception retrieving bcidsId for bcid with identifier: " + identifier, e);
        } finally {
            db.close(stmt, rs);
        }
        return bcidsId;
    }

    /**
     * Close the SQL connection
     */
    public void close() {
        db.close();
    }

    /**
     * Return a JSON representation of a bcidList
     * TODO: find a more appropriate spot for this
     *
     * @param username
     *
     * @return
     */
    public String bcidList(String username) {
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        sb.append("\"0\":\"Create new group\"");

        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            String sql = "select b.bcidId as bcidId, concat_ws(' ',identifier,title) as identifier from bcids b, users u where u.username = ? && " +
                    "b.userId=u.userId";
            stmt = conn.prepareStatement(sql);

            stmt.setString(1, username);
            rs = stmt.executeQuery();
            while (rs.next()) {
                sb.append(",\"" + rs.getInt("bcidId") + "\":\"" + rs.getString("identifier") + "\"");
            }
            sb.append("}");

        } catch (SQLException e) {
            throw new ServerErrorException("Server Error", "Exception retrieving bcids for user " + username, e);
        } finally {
            db.close(stmt, rs);
        }
        return sb.toString();
    }

    /**
     * Return an HTML table of bcids owned by a particular user
     * TODO: find a more appropriate spot for this
     *
     * @param username
     *
     * @return
     */
    public String bcidTable(String username) {
        ResourceTypes rts = new ResourceTypes();

        StringBuilder sb = new StringBuilder();
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            String sql = "SELECT \n\t" +
                    "b.bcidId as bcidId," +
                    "identifier," +
                    "ifnull(title,'') as title," +
                    "ifnull(doi,'') as doi," +
                    "ifnull(webAddress,'') as webAddress," +
                    "ifnull(resourceType,'') as resourceType," +
                    "suffixPassthrough as suffixPassthrough " +
                    "\nFROM\n\t" +
                    "bcids b, users u " +
                    "\nWHERE\n\t" +
                    "u.username = ? && " +
                    "b.userId=u.userId";
            stmt = conn.prepareStatement(sql);
            stmt.setString(1, username);

            rs = stmt.executeQuery();
            sb.append("<table>\n");
            sb.append("\t");
            sb.append("<tr>");
            sb.append("<th>BCID</th>");
            sb.append("<th>Title</th>");
            //sb.append("<th>DOI</th>");
            //sb.append("<th>webAddress</th>");
            sb.append("<th>resourceType</th>");
            sb.append("<th>Follow Suffixes</th>");

            sb.append("</tr>\n");
            while (rs.next()) {
                sb.append("\t<tr>");
                //sb.append("<td>" + getEZIDLink(rs.getString("identifier"), username) + " " + getEZIDMetadataLink(rs.getString("identifier"), username) + "</td>");
                sb.append("<td>" +
                        rs.getString("identifier") +
                        " " +
                        // Normally we would use resolverMetadataPrefix here but i'm stripping the host so this
                        // can be more easily tested on localhost
                        "(<a href='javascript:void();' class='edit' data-ark='" + rs.getString("identifier") + "'>edit</a>)" +
                        "</td>");

                sb.append("<td>" + rs.getString("title") + "</td>");
                //sb.append("<td>" + getDOILink(rs.getString("doi")) + " " + getDOIMetadataLink(rs.getString("doi")) + "</td>");
                //sb.append("<td>" + rs.getString("webAddress") + "</td>");

                ResourceType resourceType = rts.get(rs.getString("resourceType"));
                if (resourceType != null) {
                    sb.append("<td><a href='" + rs.getString("resourceType") + "'>" + resourceType.string + "</a></td>");
                } else {
                    sb.append("<td><a href='" + rs.getString("resourceType") + "'>" + rs.getString("resourceType") + "</a></td>");
                }
                sb.append("<td>" + rs.getBoolean("suffixPassthrough") + "</td>");

                sb.append("</tr>\n");
            }
            sb.append("\n</table>");

        } catch (SQLException e) {
            throw new ServerErrorException(e);
        } finally {
            db.close(stmt, rs);
        }
        return sb.toString();
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
            System.out.println(b.bcidTable("biocode"));
        } catch (Exception e) {
            b.close();
            e.printStackTrace();
        }


    }

    /**
     * fetch a BCID's metadata given a identifier and username
     *
     * @param identifier
     * @param username
     *
     * @return
     */
    public Hashtable<String, String> getBcidMetadata(String identifier, String username) {
        Hashtable<String, String> metadata = new Hashtable<String, String>();
        ResourceTypes rts = new ResourceTypes();
        Integer userId = db.getUserId(username);

        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            String sql = "SELECT suffixPassThrough as suffix, doi, title, webAddress, resourceType " +
                    "FROM bcids WHERE BINARY identifier = ? AND userId = \"" + userId + "\"";
            stmt = conn.prepareStatement(sql);

            stmt.setString(1, identifier);

            rs = stmt.executeQuery();
            if (rs.next()) {
                metadata.put("suffix", String.valueOf(rs.getBoolean("suffix")));
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
                    "bcid with identifier: " + identifier + " and userId: " + userId, e);
        } finally {
            db.close(stmt, rs);
        }
        return metadata;
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
    public Boolean updateBcidMetadata(Hashtable<String, String> metadata, String identifier, String username) {
        PreparedStatement stmt = null;
        try {
            Integer userId = db.getUserId(username);
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
                    sql += " WHERE BINARY identifier =\"" + identifier + "\" AND userId =\"" + userId + "\";";
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
                    "bcid with identifier: " + identifier + " and user: " + username, e);
        } finally {
            db.close(stmt, null);
        }
    }

    /**
     * return an HTML table to edit a Bcid's config
     *
     * @param username
     * @param identifier
     *
     * @return
     */
    public String bcidEditorAsTable(String username, String identifier) {
        StringBuilder sb = new StringBuilder();
        Hashtable<String, String> config = getBcidMetadata(identifier, username);

        sb.append("<form method=\"POST\" id=\"bcidEditForm\">\n");
        sb.append("\t<input type=hidden name=resourceTypes id=resourceTypes value=\"Dataset\">\n");
        sb.append("\t<table>\n");

        sb.append("\t\t<tr>\n");
        sb.append("\t\t\t<td align=\"right\">Title*</td>\n");
        sb.append("\t\t\t<td><input name=\"title\" type=\"textbox\" size=\"40\" value=\"");
        sb.append(config.get("title"));
        sb.append("\"></td>\n");
        sb.append("\t\t</tr>\n");

        sb.append("\t\t<tr>\n");
        sb.append("\t\t\t<td align=\"right\"><a href='/bcid/concepts.jsp'>Concept*</a></td>\n");
        sb.append("\t\t\t<td><select name=\"resourceTypesMinusDataset\" id=\"resourceTypesMinusDataset\" data-resource_type=\"");
        sb.append(config.get("resourceType"));
        sb.append("\"></select></td>\n");
        sb.append("\t\t</tr>\n");

        sb.append("\t\t<tr>\n");
        sb.append("\t\t\t<td align=\"right\">Target URL</td>\n");
        sb.append("\t\t\t<td><input name=\"webAddress\" type=\"textbox\" size=\"40\" value=\"");
        sb.append(config.get("webAddress"));
        sb.append("\"></td>\n");
        sb.append("\t\t</tr>\n");

        sb.append("\t\t<tr>\n");
        sb.append("\t\t\t<td align=\"right\">DOI</td>\n");
        sb.append("\t\t\t<td><input name=\"doi\" type=\"textbox\" size=\"40\" value=\"");
        sb.append(config.get("doi"));
        sb.append("\"></td>\n");
        sb.append("\t\t</tr>\n");

        sb.append("\t\t<tr>\n");
        sb.append("\t\t\t<td align=\"right\">Rights</td>\n");
        sb.append("\t\t\t<td><a href=\"http://creativecommons.org/licenses/by/3.0/\">Creative Commons Attribution 3.0</a></td>");
        sb.append("\t\t</tr>\n");

        sb.append("\t\t<tr>\n");
        sb.append("\t\t\t<td align=\"right\">Follow Suffixes</td>\n");
        sb.append("\t\t\t<td><input name=\"suffixPassThrough\" type=\"checkbox\"");
        if (config.get("suffix").equalsIgnoreCase("true")) {
            sb.append(" checked=\"checked\"");
        }
        sb.append("\"></td>\n");
        sb.append("\t\t</tr>\n");

        sb.append("\t\t<tr>\n");
        sb.append("\t\t\t<td></td>\n");
        sb.append("\t\t\t<td class=\"error\"></td>\n");
        sb.append("\t\t</tr>\n");

        sb.append("\t\t<tr>\n");
        sb.append("\t\t\t<td><input type=\"hidden\" name=\"identifier\" value=\"" + identifier + "\"></td>\n");
        sb.append("\t\t\t<td><input type=\"button\" value=\"Submit\" onclick=\"bcidEditorSubmit();\" /><input type=\"button\" id=\"cancelButton\" value=\"Cancel\" /></td>\n");
        sb.append("\t\t</tr>\n");

        sb.append("\t</table>\n");
        sb.append("</form>\n");

        return sb.toString();
    }

    /**
     * create Bcid's corresponding to expeditions
     */
    public String createEntityBcid(Bcid bcid) {

        String identifier = mint(
                new Integer(sm.retrieveValue("bcidNAAN")),
                bcid
        );

        // Create EZIDs right away for Bcid level Identifiers
        // Initialize ezid account
        // NOTE: On any type of EZID error, we DON'T want to fail the process.. This means we need
        // a separate mechanism on the server side to check creation of EZIDs.  This is easy enough to do
        // in the Database.
        // Never request EZID for user=demo
        if (db.getUserName(bcid.userId).equalsIgnoreCase("demo")) {
            ezidRequest = false;
        }

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
                creator.close();
            }
        }
        return identifier;
    }
}