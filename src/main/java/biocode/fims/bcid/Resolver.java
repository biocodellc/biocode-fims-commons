package biocode.fims.bcid;

import biocode.fims.digester.Mapping;
import biocode.fims.entities.Bcid;
import biocode.fims.entities.Expedition;
import biocode.fims.fimsExceptions.ServerErrorException;
import biocode.fims.repository.BcidRepository;
import biocode.fims.settings.SettingsManager;
import org.springframework.beans.factory.annotation.Autowired;

import java.net.URI;
import java.net.URISyntaxException;

/**
 * Resolves any incoming Bcid to the BCID and/or EZID systems.
 * Resolver first checks if this is a data group.  If so, it then checks if there is a decodable BCID.  If not,
 * then check if there is a suffix and if THAT is resolvable.
 */
public class Resolver {

    private BcidRepository bcidRepository;
    private SettingsManager settingsManager;

    @Autowired
    public Resolver(BcidRepository bcidRepository, SettingsManager settingsManager) {
        this.bcidRepository = bcidRepository;
        this.settingsManager = settingsManager;
    }

    /**
     * Attempt to resolve a particular identifier
     *
     * @return URI content location URL
     */
    public URI resolveIdentifier(String identifierString, Mapping mapping) {
        URI resolution = null;
        String resolverMetadataPrefix = settingsManager.retrieveValue("resolverMetadataPrefix");
        String divider = settingsManager.retrieveValue("divider");

        Identifier identifier = new Identifier(identifierString, divider);
        Bcid bcid = bcidRepository.findByIdentifier(identifier.getBcidIdentifier());

        try {
            if (bcid.getWebAddress() != null && !String.valueOf(bcid.getWebAddress()).isEmpty()) {
                resolution = new URI(bcid.getWebAddress() + identifier.getSuffix());
            } else if (mapping != null) {
                switch (bcid.getResourceType()) {
                    case Expedition.EXPEDITION_RESOURCE_TYPE:
                        // Try and get expeditionForwardingAddress in Mapping.metadata
                        String expeditionForwardingAddress = mapping.getExpeditionForwardingAddress();

                        if (expeditionForwardingAddress != null && !expeditionForwardingAddress.isEmpty()) {
                            expeditionForwardingAddress.replace("{ark}", identifier.getIdentifier());
                            resolution = new URI(expeditionForwardingAddress);

                        }
                        break;
                    case ResourceTypes.DATASET_RESOURCE_TYPE:
                        String conceptForwardingAddress = mapping.getConceptForwardingAddress();

                        if (conceptForwardingAddress!= null && !conceptForwardingAddress.isEmpty()) {
                            conceptForwardingAddress.replace("{ark}", identifier.getIdentifier());
                            resolution = new URI(conceptForwardingAddress);
                        }

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
}
