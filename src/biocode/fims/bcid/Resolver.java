package biocode.fims.bcid;

import biocode.fims.bcid.Renderer.JSONRenderer;
import biocode.fims.bcid.Renderer.Renderer;
import biocode.fims.ezid.EzidException;
import biocode.fims.ezid.EzidService;
import biocode.fims.fimsExceptions.BadRequestException;
import biocode.fims.fimsExceptions.ServerErrorException;
import biocode.fims.settings.SettingsManager;
import biocode.fims.utils.Timer;

import java.math.BigInteger;
import java.net.URI;
import java.net.URISyntaxException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Resolves any incoming Bcid to the BCID and/or EZID systems.
 * Resolver first checks if this is a data group.  If so, it then checks if there is a decodable BCID.  If not,
 * then check if there is a suffix and if THAT is resolvable.
 */
public class Resolver extends Database {
    String identifier = null;
    String scheme = "ark:";
    String naan = null;
    String shoulder = null;     // The data group
    String suffix = null;        // The local Bcid
    BigInteger element_id = null;
    Integer bcidsId = null;
    public boolean forwardingResolution = false;
    public String graph = null;
    static SettingsManager sm;

    /**
     * Load settings manager, set ontModelSpec.
     */
    static {
        // Initialize settings manager
        sm = SettingsManager.getInstance();
    }

    private String project;

    /**
     * Pass an identifier to the Resolver
     *
     * @param identifier
     */
    public Resolver(String identifier) {
        super();


        this.identifier = identifier;
        // Pull off potential last piece of string which would represent the local Identifier
        // The piece to decode is ark:/NAAN/bcidIdentifer (anything else after a last trailing "/" not decoded)
        StringBuilder stringBuilder = new StringBuilder();

        String bits[] = identifier.split("/", 3);
        // just want the first chunk between the "/"'s
        naan = bits[1];
        // Now decipher the shoulder and suffix in the next bit
        setShoulderAndSuffix(bits[2]);
        // Call setBcid() to set bcidsId
        setBcid();
    }

    /**
     * Find the appropriate BCID ROOT for this expedition given an conceptAlias.
     *
     * @param expeditionCode defines the BCID expeditionCode to lookup
     * @param conceptAlias    defines the alias to narrow this,  a one-word reference denoting a BCID
     *
     * @return returns the BCID for this expedition and conceptURI combination
     */
    public Resolver(String expeditionCode, Integer projectId, String conceptAlias) {
        ResourceTypes resourceTypes = new ResourceTypes();
        ResourceType rt = resourceTypes.getByShortName(conceptAlias);
        String uri = rt.uri;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            String query = "select \n" +
                    "b.identifier as identifier \n" +
                    "from \n" +
                    "bcids b, expeditionBcids eb, expeditions p \n" +
                    "where \n" +
                    "b.bcidId=eb.bcidId&& \n" +
                    "eb.expeditionId=p.expeditionId && \n" +
                    "p.expeditionCode= ? && \n" +
                    "p.projectId = ? && \n" +
                    "(b.resourceType=? || b.resourceType= ?)";
            stmt = conn.prepareStatement(query);

            stmt.setString(1, expeditionCode);
            stmt.setInt(2, projectId);
            stmt.setString(3, uri);
            stmt.setString(4, conceptAlias);


            //System.out.println("Resolver query = " + query);
            rs = stmt.executeQuery();
            rs.next();
            this.identifier = rs.getString("identifier");
        } catch (SQLException e) {
            throw new ServerErrorException(e);
        } finally {
            close(stmt, rs);
        }
    }


    public String getIdentifier() {
        return identifier;
    }

    /**
     * Return an Bcid representing a data set
     *
     * @return
     */
    public Integer getBcidId () {
        return bcidsId;
    }

    /**
     * Return an Bcid representing a data element
     *
     * @return
     */
    public BigInteger getElementID() {
        return element_id;
    }

    /**
     * Set the shoulder and suffix variables for this identifier
     *
     * @param a
     */
    private void setShoulderAndSuffix (String a) {
        boolean reachedShoulder = false;
        StringBuilder sbShoulder = new StringBuilder();
        StringBuilder sbSuffix = new StringBuilder();

        for (int i = 0; i < a.length(); i++) {
            char c = a.charAt(i);
            if (!reachedShoulder)
                sbShoulder.append(c);
            else
                sbSuffix.append(c);
            if (Character.isDigit(c))
                reachedShoulder = true;
        }
        shoulder = sbShoulder.toString();
        suffix = sbSuffix.toString();

        // String the slash between the shoulder and the suffix
        if (!sm.retrieveValue("divider").equals("")) {
            if (suffix.startsWith(sm.retrieveValue("divider"))) {
                suffix = suffix.substring(1);
            }
        }
    }

    /**
     * Attempt to resolve a particular identifier.
     *
     * @return URI content location URL
     */
    public URI resolveIdentifier() throws URISyntaxException {
        Bcid bcid;
        URI resolution;

        // First  option is check if Bcid, then look at other options after this is determined
        if (!isValidBCID()) {
            throw new BadRequestException("Invalid bcid.");
        }

        bcid = new Bcid(suffix, bcidsId);

        // A resolution target is specified AND there is a suffix AND suffixPassThrough
        if (bcid.forwardingResolution) {
            forwardingResolution = true;

            // Immediately return resolution result
            resolution = bcid.resolutionTarget;
        } else {
            resolution = bcid.getMetadataTarget();

            this.project = getProjectID(bcidsId);
        }

        return resolution;
    }

    /**
     * Print Metadata for a particular Bcid
     *
     * @return JSON String with content for the interface
     */
    public String printMetadata(Renderer renderer) {
        Bcid bcid = null;

        // First  option is check if Bcid, then look at other options after this is determined
        if (setBcid()) {

            bcid = new Bcid(bcidsId);

            if (suffix != null && bcid.getWebAddress() != null) {
                bcid = new Bcid(suffix, bcid.getWebAddress(), bcidsId);
            }
        }
        return renderer.render(bcid);
    }

    /**
     * Resolve an EZID version of this identifier
     *
     * @param ezidService
     *
     * @return JSON string to send to interface
     */
    public String resolveEZID(EzidService ezidService, Renderer renderer) {
        // First fetch from EZID, and populate a map
        Ezid ezid = null;

        try {
            ezid = new Ezid(ezidService.getMetadata(identifier));
        } catch (EzidException e) {
            //TODO should we silence this exception?
            logger.warn("URISyntaxException thrown", e);
        }
        return renderer.render(ezid);
    }


    /**
     * Resolve identifiers through BCID AND EZID -- This method assumes JSONRenderer
     *
     * @param ezidService
     *
     * @return JSON string with information about BCID/EZID results
     */
    public String resolveAllAsJSON(EzidService ezidService) {
        Timer t = new Timer();
        Renderer renderer = new JSONRenderer();
        StringBuilder sb = new StringBuilder();
        sb.append("[\n");

        try {
            sb.append("  " + this.resolveIdentifier().toString());
        } catch (URISyntaxException e) {
            //TODO should we silence this exception?
            logger.warn("URISyntaxException thrown", e);
        }
        t.lap("resolveIdentifier");
        sb.append("\n  ,\n");
        sb.append("  " + this.resolveEZID(ezidService, renderer));
        t.lap("resolveEZID");
        sb.append("\n]");
        return sb.toString();
    }

    private boolean isValidBCID() {
        if (bcidsId != null)
            return true;
        else
            return false;
    }

    /**
     * Check if this is a Bcid and set the bcidsId
     *
     * @return
     */
    private boolean setBcid() {
        // Test Bcid is #1
        if (shoulder.equals("fk4") && naan.equals("99999")) {
            bcidsId = 1;
            return true;
        }

        // Decode a typical Bcid
        bcidsId = new BcidEncoder().decode(shoulder).intValue();

        if (bcidsId == null) {
            return false;
        } else {
            // Now we need to figure out if this bcidId exists or not in the Database
            String select = "SELECT count(*) as count FROM bcids where bcidId = ?";
            PreparedStatement stmt = null;
            ResultSet rs = null;
            try {
                stmt = conn.prepareStatement(select);
                stmt.setInt(1, bcidsId);
                rs = stmt.executeQuery();
                rs.next();
                int count = rs.getInt("count");
                if (count < 1) {
                    bcidsId = null;
                    return false;
                } else {
                    return true;
                }
            } catch (SQLException e) {
                throw new ServerErrorException(e);
            } finally {
                close(stmt, rs);
            }
        }
    }

    /**
     * Get the projectId given a bcidsId
     *
     * @param bcidsId
     */
    public String getProjectID(Integer bcidsId) {
        String projectId = "";
        String sql = "select p.projectId from projects p, expeditionBcids eb, expeditions e, " +
                "bcids b where b.bcidId = eb.bcidId and e.expeditionId=eb.`expeditionId` " +
                "and e.`projectId`=p.`projectId` and b.bcidId = ?";

        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            stmt = conn.prepareStatement(sql);
            stmt.setInt(1, bcidsId);
            rs = stmt.executeQuery();
            rs.next();
            projectId = rs.getString("projectId");

        } catch (SQLException e) {
            // catch the exception and log it
            logger.warn("Exception retrieving projectCode for bcid: " + bcidsId, e);
        } finally {
            close(stmt, rs);
        }
        return projectId;
    }

    /**
     * Main function for testing.
     *
     * @param args
     */
    public static void main(String args[]) {
        Resolver r = null;
        SettingsManager sm = SettingsManager.getInstance("biocode-fims.props");

        try {
            //r = new Resolver("ark:/21547/S2MBIO56");
            r = new Resolver("ark:/21547/fR2");
            System.out.println("  " + r.resolveIdentifier());
            System.out.println(r.resolveIdentifierAs("tab"));

        } catch (Exception e) {
            e.printStackTrace();
        }
         /*
        try {
            r = new Resolver("ark:/87286/C2/64c82d19-6562-4174-a5ea-e342eae353e8");
            System.out.println("  " + r.resolveIdentifier());
        } catch (Exception e) {
            e.printStackTrace();
        }
          */

        try {
            String expected = "";
            // suffixpassthrough = 1; no webAddress specified; has a Suffix
            /*r = new Resolver("ark:/87286/U264c82d19-6562-4174-a5ea-e342eae353e8");
            expected = "http://biscicol.org/id/metadata/ark:/21547/U264c82d19-6562-4174-a5ea-e342eae353e8";
            System.out.println(r.resolveIdentifier());
                  */
            // suffixPassthrough = 1; webAddress specified; has a Suffix
            //r = new Resolver("ark:/21547/R2");
            //System.out.println(r.printMetadata(new RDFRenderer()));
            //System.out.println(r.resolveIdentifier());

            //System.out.println(r.resolveIdentifier());
            //expected = "http://biocode.berkeley.edu/specimens/MBIO56";
            //System.out.println(r.printMetadata(new RDFRenderer()));

           /* r = new Resolver("DEMO4",18,"Resource");
            //System.out.println(r.resolveIdentifier());
           System.out.println(r.getArk());
           */
                 /*
            // suffixPassthrough = 1; webAddress specified; no Suffix
            r = new Resolver("ark:/21547/R2");
            expected = "http://biscicol.org/id/metadata/ark:/21547/R2";
            System.out.println(r.resolveIdentifier());

            // suffixPassthrough = 0; no webAddress specified; no Suffix
            r = new Resolver("ark:/21547/W2");
            expected = "http://biscicol.org/id/metadata/ark:/21547/W2";
            System.out.println(r.resolveIdentifier());

            // suffixPassthrough = 0; webAddress specified; no Suffix
            r = new Resolver("ark:/21547/Gk2");
            expected =  "http://biscicol.org:3030/ds?graph=urn:uuid:77806834-a34f-499a-a29f-aaac51e6c9f8";
            System.out.println(r.resolveIdentifier());

               // suffixPassthrough = 0; webAddress specified;  suffix specified (still pass it through
            r = new Resolver("ark:/21547/Gk2FOO");
            expected =  "http://biscicol.org:3030/ds?graph=urn:uuid:77806834-a34f-499a-a29f-aaac51e6c9f8FOO";
            System.out.println(r.resolveIdentifier());
                 */
            // EzidService service = new EzidService();
            // service.login(sm.retrieveValue("eziduser"), sm.retrieveValue("ezidpass"));
            //System.out.println(r.);
            //Renderer ren = new RDFRenderer();
            //System.out.println(r.printMetadata(ren));

            //r = new Resolver("DEMOH", 1, "Sequencing");
            //System.out.println(r.getArk());
            //System.out.println(r.resolveIdentifier().toString());
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            r.close();
        }


        /* String result = null;
        try {
            result = URLDecoder.decode("ark%3A%2F87286%2FC2", "UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        try {
            r = new Resolver(result);
            r.resolveIdentifier();
            System.out.println(r.ark + " : " + r.datasets_id);
            //    EzidService service = new EzidService();
            //    System.out.println("  " + r.resolveAll(service));
        } catch (Exception e) {
            e.printStackTrace();
        }
        */

    }

    /**
     * Return a graph in a particular format
     *
     * @param format
     *
     * @return
     */
    public URI resolveIdentifierAs(String format) throws URISyntaxException {
        // Example
        //http://biscicol.org:8179/biocode-fims/rest/query/tab?graphs=urn:uuid:ec90c3b6-cc75-4090-b03d-cf3d76a27783&projectId=1

        String contentResolutionRoot = sm.retrieveValue("contentResolutionRoot");
        return new URI(contentResolutionRoot + format + "?graphs=" + graph + "&projectId=" + project);
    }

    public String getExpeditionCode() {
        String expeditionCode = "";

        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            String sql = "select e.expeditionCode from expeditionBcids eb, expeditions e, bcids b " +
                    "where b.bcidId = eb.bcidId and e.expeditionId=eb.`expeditionId` and b.bcidId = ?";

            stmt = conn.prepareStatement(sql);
            stmt.setInt(1, bcidsId);
            rs = stmt.executeQuery();
            rs.next();
            expeditionCode = rs.getString("expeditionCode");

        } catch (SQLException e) {
            // catch the exception and log it
            logger.warn("Exception retrieving expeditionCode for bcid: " + bcidsId, e);
        } finally {
            close(stmt, rs);
        }
        return expeditionCode;
    }

    public Integer getExpeditionId() {
        Integer expeditionId = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            String sql = "select eb.expeditionId from expeditionBcids eb, bcids b " +
                    "where b.bcidId = eb.bcidId and b.bcidId = ?";

            stmt = conn.prepareStatement(sql);
            stmt.setInt(1, bcidsId);
            rs = stmt.executeQuery();
            rs.next();
            expeditionId = rs.getInt("eb.expeditionId");

        } catch (SQLException e) {
            // catch the exception and log it
            logger.warn("Exception retrieving expeditionId for bcid: " + bcidsId, e);
        } finally {
            close(stmt, rs);
        }
        return expeditionId;
    }
}
