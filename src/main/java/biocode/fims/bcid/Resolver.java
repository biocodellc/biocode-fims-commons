package biocode.fims.bcid;

import biocode.fims.digester.Mapping;
import biocode.fims.entities.Bcid;
import biocode.fims.entities.Expedition;
import biocode.fims.fimsExceptions.ServerErrorException;
import biocode.fims.repository.BcidRepository;
import biocode.fims.settings.SettingsManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.propertyeditors.StringArrayPropertyEditor;

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

        boolean hasWebAddress = (bcid.getWebAddress() != null && !String.valueOf(bcid.getWebAddress()).isEmpty());

        try {
            switch (bcid.getResourceType()) {
                case Expedition.EXPEDITION_RESOURCE_TYPE:
                    if (hasWebAddress)
                        resolution = new URI(bcid.getWebAddress() + String.valueOf(bcid.getIdentifier()));
                    else {
                        // Try and get expeditionForwardingAddress in Mapping.metadata
                        String expeditionForwardingAddress = mapping.getExpeditionForwardingAddress();

                        if (expeditionForwardingAddress != null && !expeditionForwardingAddress.isEmpty()) {
                            String appendedExpeditionForwardingAddress = expeditionForwardingAddress.replace(
                                    "{ark}", String.valueOf(bcid.getIdentifier()));
                            resolution = new URI(appendedExpeditionForwardingAddress);
                        }
                    }
                    break;
                case ResourceTypes.DATASET_RESOURCE_TYPE:
                    if (hasWebAddress)
                        resolution = bcid.getWebAddress();
                    break;
                default:
                    if (identifier.hasSuffix()) {
                        if (hasWebAddress)
                            resolution = new URI(bcid.getWebAddress() + String.valueOf(identifier.getSuffix()));
                        else {
                            String conceptForwardingAddress = mapping.getConceptForwardingAddress();

                            if (conceptForwardingAddress != null && !conceptForwardingAddress.isEmpty()) {
                                String appendedConceptForwardingAddress = conceptForwardingAddress.replace(
                                        "{ark}", bcid.getIdentifier() + "/" + identifier.getSuffix());
                                resolution = new URI(appendedConceptForwardingAddress);
                            }
                        }
                    }

                    break;
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
