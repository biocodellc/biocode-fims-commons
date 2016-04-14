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
import org.springframework.beans.factory.annotation.Configurable;
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
@Configurable
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
                resolution = new URI(resolverMetadataPrefix + bcid.getIdentifier());
            }
        } catch(URISyntaxException e) {
            throw new ServerErrorException("Server Error", "Syntax exception thrown for metadataTargetPrefix: \"" +
                    resolverMetadataPrefix + "\" and bcid: \"" + bcid + "\"", e);
        }

        return resolution;
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
