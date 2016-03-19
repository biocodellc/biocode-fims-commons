package biocode.fims.bcid;

import biocode.fims.fimsExceptions.BadRequestException;
import biocode.fims.fimsExceptions.ServerErrorException;
import biocode.fims.settings.SettingsManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.URISyntaxException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;

/**
 * The Bcid class encapsulates all of the information we know about a BCID.
 * This includes data such as the
 * status of EZID creation, associated Bcid calls, and any metadata.
 * It can include a data element or a data group.
 * There are several ways to construct an element, including creating it from scratch, or instantiating by looking
 * up an existing Bcid from the Database.
 */
public class Bcid {

    protected URI prefix;        // if the bcid object supports suffixPassThrough and there is a suffix, this is the bcid identifier
    protected URI identifier;
    protected String suffix;       // Source or local Bcid (e.g. MBIO056)
    protected URI webAddress;        // URI for the webAddress, EZID calls this _target (e.g. http://biocode.berkeley.edu/specimens/MBIO56)
    protected String resourceType;           // erc.what
    protected String who;            // erc.who
    protected String title;            // erc.who\
    protected String projectCode;
    protected Boolean ezidMade;
    protected Boolean ezidRequest;
    protected String ts;
    protected Boolean suffixPassThrough = false;
    protected String doi;
    protected Integer bcidId;
    protected Boolean isPublic;
    protected Integer userId;
    protected Boolean finalCopy;

    public String getGraph() {
        return graph;
    }

    protected String graph;
    protected Boolean forwardingResolution = false;
    protected URI resolutionTarget;

    protected static String rights;
    protected static String resolverTargetPrefix;
    protected static String resolverMetadataPrefix;

    // HashMap to store metadata values
    private HashMap<String, String> map = new HashMap<String, String>();


    static SettingsManager sm;

    private static Logger logger = LoggerFactory.getLogger(Bcid.class);

    static {
        sm = SettingsManager.getInstance();

        rights = sm.retrieveValue("rights");
        resolverTargetPrefix = sm.retrieveValue("resolverTargetPrefix");
        resolverMetadataPrefix = sm.retrieveValue("resolverMetadataPrefix");

    }

    protected Bcid() {
    }

    /**
     * constructor used for creating a new bcid
     */
    public Bcid(Integer userId, String resourceType, String title, String webAddress, String graph, String doi, Boolean finalCopy, Boolean suffixPassThrough) {
        this.userId = userId;
        this.resourceType = resourceType;
        this.suffixPassThrough = suffixPassThrough;
        this.doi = doi;
        if (webAddress != null) {
            try {
                this.webAddress = new URI(webAddress);
            } catch (URISyntaxException e) {
                throw new BadRequestException("Malformed uri: " + webAddress, e);
            }
        }
        this.graph = graph;
        this.title = title;
        this.finalCopy = finalCopy;
    }

    /**
     * Create data group
     *
     * @param bcidId
     */
    public Bcid(Integer bcidId) {
        getBcid(bcidId);
    }


    /**
     * Create an element given a source Bcid, and a resource type Bcid
     *
     * @param suffix
     * @param bcidId
     */
    public Bcid(String suffix, Integer bcidId) {
        this.suffix = suffix;
        getBcid(bcidId);
        BcidMinter bcidMinter = new BcidMinter();
        try {
            projectCode = bcidMinter.getProject(bcidId);
        } catch (Exception e) {
            System.out.println("project code has not been set");
            projectCode = "Unassigned to a project";
        }
    }

    /**
     * Create an element given a source Bcid, web address for resolution, and a bcidId
     * This method is meant for CREATING bcids.
     *
     * @param suffix
     * @param webAddress
     * @param bcidId
     */
    public Bcid(String suffix, URI webAddress, Integer bcidId) {
        this.suffix = suffix;
        getBcid(bcidId);
        this.webAddress = webAddress;

        // Reformat webAddress in this constructor if there is a suffix
        if (suffix != null && webAddress != null && !suffix.toString().trim().equals("") && !webAddress.toString().trim().equals("")) {
            //System.out.println("HERE" + webAddress);
            try {
                this.webAddress = new URI(webAddress + suffix);
            } catch (URISyntaxException e) {
                //TODO should we silence this exception?
                logger.warn("URISyntaxException for uri: {}", webAddress + suffix, e);
            }
        }
    }


    /**
     * Internal functional for fetching the Bcid given the bcidId
     *
     * @param pBcidId
     */
    private void getBcid(Integer pBcidId) {
        Connection conn = BcidDatabase.getConnection();
        PreparedStatement stmt = null;
        ResultSet rs = null;
        String sql = "SELECT " +
                "b.identifier as identifier," +
                "b.ezidRequest as ezidRequest," +
                "b.ezidMade as ezidMade," +
                "b.suffixPassthrough as suffixPassthrough," +
                "b.doi as doi," +
                "b.title as title," +
                "b.ts as ts, " +
                "CONCAT_WS(' ',u.firstName, u.lastName) as who, " +
                "b.webAddress as webAddress," +
                "b.graph as graph," +
                "b.resourceType as resourceType" +
                " FROM bcids b, users u " +
                " WHERE b.bcidId = ?" +
                " AND b.userId = u.userId ";

        try {
            stmt = conn.prepareStatement(sql);
            stmt.setInt(1, pBcidId);

            rs = stmt.executeQuery();
            rs.next();

            identifier = new URI(rs.getString("identifier"));
            ezidRequest = rs.getBoolean("ezidRequest");
            ezidMade = rs.getBoolean("ezidMade");
            doi = rs.getString("doi");
            title = rs.getString("title");
            ts = rs.getString("ts");
            who = rs.getString("who");
            suffixPassThrough = rs.getBoolean("suffixPassthrough");
            resourceType = rs.getString("resourceType");
            graph = rs.getString("graph");

            // set the prefix
            if (suffixPassThrough && suffix != null && !suffix.equals("")) {
                prefix = identifier;
                identifier = new URI(prefix + sm.retrieveValue("divider") + suffix);
            } else {
                prefix = identifier;
            }

            String lWebAddress = rs.getString("webAddress");
            if (lWebAddress != null && !lWebAddress.trim().equals("")) {
                try {
                    webAddress = new URI(lWebAddress);
                } catch (URISyntaxException e) {
                    logger.warn("URISyntaxException with uri: {} and bcidId: {}", rs.getString("webAddress"),
                            this.bcidId, e);
                }
            }
            // A resolution target is specified AND there is a suffix AND suffixPassThrough
            if (webAddress != null && !webAddress.equals("") &&
                    suffix != null && !suffix.trim().equals("") && suffixPassThrough) {
                forwardingResolution = true;

                // Set the resolution target
                resolutionTarget = new URI(webAddress + suffix);
            }

            bcidId = pBcidId;
            setIsPublic();
        } catch (SQLException e) {
            throw new ServerErrorException(e);
        } catch (URISyntaxException e) {
            throw new ServerErrorException("Server Error","URISyntaxException from identifier: " + prefix +
                    sm.retrieveValue("divider") + suffix + " from bcidId: " + bcidId, e);
        } finally {
            BcidDatabase.close(conn, stmt, rs);
        }
    }

    /**
     * method to set the bcid isPublic variable. We can't do this with the getBcid query as it was returning null whne
     * a bcid isn't associated with an expedition
     */
    private void setIsPublic() {
        Connection conn = BcidDatabase.getConnection();
        PreparedStatement stmt = null;
        ResultSet rs = null;
        String sql = "SELECT " +
                "e.public" +
                " FROM expeditions e, expeditionBcids eb" +
                " WHERE eb.bcidId = ?" +
                " AND e.expeditionId = eb.expeditionId";

        try {
            stmt = conn.prepareStatement(sql);
            stmt.setInt(1, bcidId);

            rs = stmt.executeQuery();
            if (rs.next())
                isPublic = rs.getBoolean("e.public");
        } catch (SQLException e) {
            throw new ServerErrorException(e);
        } finally {
            BcidDatabase.close(conn, stmt, rs);
        }
    }

    public URI getWebAddress() {
        return webAddress;
    }

    public URI getMetadataTarget() throws URISyntaxException {
        return new URI(resolverMetadataPrefix + getIdentifier());
    }

    private void put(String key, String val) {
        if (val != null)
            map.put(key, val);
    }

    private void put(String key, Boolean val) {
        if (val != null)
            map.put(key, val.toString());
    }

    private void put(String key, URI val) {
        if (val != null) {
            map.put(key, val.toString());
        }
    }

    public Boolean getSuffixPassThrough() {
        return suffixPassThrough;
    }

    /**
     * method to return the identifier of the Bcid.
     * @return
     */
    public URI getIdentifier() {
        return identifier;
    }

    public Integer getBcidId() {
        return bcidId;
    }

    /**
     * Convert the class variables to a HashMap of metadata.
     *
     * @return
     */
    public HashMap<String, String> getMetadata() {
        put("identifier", identifier);
        put("who", who);
        put("resourceType", resourceType);
        put("webAddress", webAddress);
        put("title", title);
        put("projectCode", projectCode);
        put("suffix", suffix);
        put("doi", doi);
        put("ezidMade", ezidMade);
        put("bcidsSuffixPassThrough", suffixPassThrough);
        put("ezidRequest", ezidRequest);
        put("prefix", prefix);
        put("ts", ts);
        put("rights", rights);
        put("forwardingResolution", forwardingResolution);
        if (isPublic != null)
            put("isPublic", isPublic);
        else
            put("isPublic", "null");
        if (resolutionTarget != null) {
            put("resolutionTarget", resolutionTarget);
        } else {
            put("resolutionTarget", "");
        }
        return map;
    }
}

