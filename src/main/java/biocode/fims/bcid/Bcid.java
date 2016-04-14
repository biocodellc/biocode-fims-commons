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
    protected String doi;
    protected Integer bcidId;
    protected Boolean isPublic;
    protected Integer userId;
    protected Boolean finalCopy;

    public String getGraph() {
        return graph;
    }

    protected String graph;

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
    public Bcid(Integer userId, String resourceType, String title, String webAddress, String graph, String doi, Boolean finalCopy) {
        this.userId = userId;
        this.resourceType = resourceType;
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
     * temporary constructor while we switch to the new Bcid Entity
     * @param bcid
     */
    public Bcid(biocode.fims.entities.Bcid bcid) {

        bcidId = bcid.getBcidId();
        identifier = bcid.getIdentifier();
        ezidRequest = bcid.isEzidRequest();
        ezidMade = bcid.isEzidMade();
        doi = bcid.getDoi();
        title = bcid.getTitle();
        ts = bcid.getTs().toString();
        // TODO this should be a first + last name of the User
        who = BcidDatabase.getUserName(bcid.getUserId());
        resourceType = bcid.getResourceType();
        graph = bcid.getGraph();
        webAddress = bcid.getWebAddress();

        setIsPublic();
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

    // TODO when refactoring this, if there is a suffix, we should add that
    public URI getMetadataTarget() throws URISyntaxException {
        return new URI(resolverMetadataPrefix + identifier);
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
        put("ezidRequest", ezidRequest);
        put("prefix", prefix);
        put("ts", ts);
        put("rights", rights);
        if (isPublic != null)
            put("isPublic", isPublic);
        else
            put("isPublic", "null");
        return map;
    }
}

