package biocode.fims.bcid;

import biocode.fims.digester.Mapping;
import biocode.fims.entities.Bcid;
import biocode.fims.entities.Expedition;
import biocode.fims.fimsExceptions.ServerErrorException;
import biocode.fims.repository.BcidRepository;
import biocode.fims.repository.ExpeditionRepository;
import biocode.fims.settings.SettingsManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import java.net.URI;
import java.net.URISyntaxException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Resolves any incoming Bcid to the BCID and/or EZID systems.
 * Resolver first checks if this is a data group.  If so, it then checks if there is a decodable BCID.  If not,
 * then check if there is a suffix and if THAT is resolvable.
 */
public class Resolver {
    final static Logger logger = LoggerFactory.getLogger(Resolver.class);
    private final IdentifierIncludingSuffix identifierIncludingSuffix;

    private biocode.fims.entities.Bcid bcid;


    @Autowired
    BcidRepository bcidRepository;
    @Autowired
    ExpeditionRepository expeditionRepository;
    @Autowired
    public SettingsManager settingsManager;

    /**
     * Pass an identifierIncludingSuffix to the Resolver
     *
     * @param identifierIncludingSuffix
     */
    public Resolver(String identifierIncludingSuffix) {

        this.identifierIncludingSuffix = new IdentifierIncludingSuffix(identifierIncludingSuffix);
        String identifier = this.identifierIncludingSuffix.getBcidIdentifier();

        bcid = bcidRepository.findByIdentifier(identifier);
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
        Expedition expedition = expeditionRepository.findByExpeditionCodeAndProjectId(expeditionCode, projectId);

        ResourceTypes resourceTypes = new ResourceTypes();
        ResourceType rt = resourceTypes.getByShortName(conceptAlias);
        String uri = rt.uri;

        this.bcid = bcidRepository.findByExpeditionAndResourceType(
                expedition.getExpeditionId(),
                conceptAlias, uri
        ).iterator().next();

        this.identifierIncludingSuffix = new IdentifierIncludingSuffix(this.bcid.getIdentifier().toString());
    }

    public String getIdentifier() {
        return String.valueOf(bcid.getIdentifier());
    }

    /**
     * Return an Bcid representing a data set
     *
     * @return
     */
    public Integer getBcidId () {
        return bcid.getBcidId();
    }

    /**
     * Attempt to resolve a particular identifierIncludingSuffix.
     *
     * @return URI content location URL
     */
    public URI resolveIdentifier(Mapping mapping) {
        URI resolution = null;
        String resolverMetadataPrefix = settingsManager.retrieveValue("resolverMetadataPrefix");

        try {
            if (bcid.getWebAddress() != null) {
                resolution = new URI(bcid.getWebAddress() + identifierIncludingSuffix.suffix);
            } else if (mapping != null) {
                switch (bcid.getResourceType()) {
                    case Expedition.EXPEDITION_RESOURCE_TYPE:
                        // Try and get expeditionForwardingAddress in Mapping.metadata
                        String expeditionForwardingAddress = mapping.getExpeditionForwardingAddress();

                        if (expeditionForwardingAddress != null && !expeditionForwardingAddress.isEmpty())
                            expeditionForwardingAddress.replace("{ark}", identifierIncludingSuffix.identifierIncludingSuffix);
                            resolution = new URI(expeditionForwardingAddress);

                        break;
                    case ResourceTypes.DATASET_RESOURCE_TYPE:
                        String conceptForwardingAddress = mapping.getConceptForwardingAddress();

                        if (conceptForwardingAddress!= null && !conceptForwardingAddress.isEmpty())
                            conceptForwardingAddress.replace("{ark}", identifierIncludingSuffix.identifierIncludingSuffix);
                        resolution = new URI(conceptForwardingAddress);

                        break;
                }
            }

            // if resolution is still null, then resolve to the default metadata service
            if (resolution == null) {
                resolution = new URI(resolverMetadataPrefix + getIdentifier());
            }
        } catch(URISyntaxException e) {
            throw new ServerErrorException("Server Error", "Syntax exception thrown for metadataTargetPrefix: \"" +
                    resolverMetadataPrefix + "\" and bcid: \"" + bcid + "\"", e);
        }

        return resolution;
    }

    /**
     * Get the projectId given a bcidId
     *
     * @param bcidId
     */
    public String getProjectID(Integer bcidId) {
        String projectId = "";
        String sql = "select p.projectId from projects p, expeditionBcids eb, expeditions e, " +
                "bcids b where b.bcidId = eb.bcidId and e.expeditionId=eb.`expeditionId` " +
                "and e.`projectId`=p.`projectId` and b.bcidId = ?";

        PreparedStatement stmt = null;
        ResultSet rs = null;
        Connection conn = BcidDatabase.getConnection();
        try {
            stmt = conn.prepareStatement(sql);
            stmt.setInt(1, bcidId);
            rs = stmt.executeQuery();
            rs.next();
            projectId = rs.getString("projectId");

        } catch (SQLException e) {
            // catch the exception and log it
            logger.warn("Exception retrieving projectCode for bcid: " + bcidId, e);
        } finally {
            BcidDatabase.close(conn, stmt, rs);
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
        ApplicationContext applicationContext = new ClassPathXmlApplicationContext("/applicationContext.xml");

        try {
            //r = new Resolver("ark:/21547/S2MBIO56");
            r = new Resolver("ark:/21547/Ala2");
//            System.out.println("  " + r.resolveIdentifier());
//            System.out.println(r.resolveIdentifierAs("tab"));

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
            // service.login(settingsManager.retrieveValue("eziduser"), settingsManager.retrieveValue("ezidpass"));
            //System.out.println(r.);
            //Renderer ren = new RDFRenderer();
            //System.out.println(r.printMetadata(ren));

            //r = new Resolver("DEMOH", 1, "Sequencing");
            //System.out.println(r.getArk());
            //System.out.println(r.resolveIdentifier().toString());
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
//            r.close();
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

    public String getExpeditionCode() {
        String expeditionCode = "";

        PreparedStatement stmt = null;
        ResultSet rs = null;
        Connection conn = BcidDatabase.getConnection();
        try {
            String sql = "select e.expeditionCode from expeditionBcids eb, expeditions e, bcids b " +
                    "where b.bcidId = eb.bcidId and e.expeditionId=eb.`expeditionId` and b.bcidId = ?";

            stmt = conn.prepareStatement(sql);
            stmt.setInt(1, bcid.getBcidId());
            rs = stmt.executeQuery();
            rs.next();
            expeditionCode = rs.getString("expeditionCode");

        } catch (SQLException e) {
            // catch the exception and log it
            logger.warn("Exception retrieving expeditionCode for bcid: " + bcid.getBcidId(), e);
        } finally {
            BcidDatabase.close(conn, stmt, rs);
        }
        return expeditionCode;
    }

    public Integer getExpeditionId() {
        Integer expeditionId = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        Connection conn = BcidDatabase.getConnection();
        try {
            String sql = "select eb.expeditionId from expeditionBcids eb, bcids b " +
                    "where b.bcidId = eb.bcidId and b.bcidId = ?";

            stmt = conn.prepareStatement(sql);
            stmt.setInt(1, bcid.getBcidId());
            rs = stmt.executeQuery();
            rs.next();
            expeditionId = rs.getInt("eb.expeditionId");

        } catch (SQLException e) {
            // catch the exception and log it
            logger.warn("Exception retrieving expeditionId for bcid: " + bcid.getBcidId(), e);
        } finally {
            BcidDatabase.close(conn, stmt, rs);
        }
        return expeditionId;
    }

    public Bcid getBcid() {
        return bcid;
    }

    private class IdentifierIncludingSuffix {
        private String naan;
        private String shoulder;
        private String suffix;
        private String identifierIncludingSuffix;
        private String divider;

        public IdentifierIncludingSuffix(String identifierIncludingSuffix) {
            this.identifierIncludingSuffix = identifierIncludingSuffix;
            divider = settingsManager.retrieveValue("divider");

            decode(identifierIncludingSuffix);
        }

        private void decode(String identifierIncludingSuffix) {
            // Pull off potential last piece of string which would represent the local Identifier
            // The piece to decode is ark:/NAAN/bcidIdentifer (anything else after a last trailing "/" not decoded)
            String bits[] = identifierIncludingSuffix.split("/", 3);

            // the naan is the first chunk between the "/"'s
            naan = bits[1];
            // Now decipher the shoulder and suffix in the next bit
            setShoulderAndSuffix(bits[2]);
        }

        /**
         * Set the shoulder and suffix variables for this identifierIncludingSuffix
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
            if (!divider.equals("")) {
                if (suffix.startsWith(divider)) {
                    suffix = suffix.substring(1);
                }
            }
        }

        /**
         * get the {@link biocode.fims.entities.Bcid} identifier
         * @return
         */
        public String getBcidIdentifier() {
            return "ark:/" + naan + "/" + shoulder;
        }
    }
}
