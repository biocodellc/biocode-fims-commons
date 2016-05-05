package biocode.fims.bcid;

import biocode.fims.digester.Mapping;
import biocode.fims.entities.Bcid;
import biocode.fims.entities.Expedition;
import biocode.fims.fimsExceptions.ServerErrorException;
import biocode.fims.repositories.BcidRepository;
import biocode.fims.service.BcidService;
import biocode.fims.settings.SettingsManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.net.URISyntaxException;

/**
 * Resolves any incoming Bcid to the BCID and/or EZID systems.
 * Resolver first checks if this is a data group.  If so, it then checks if there is a decodable BCID.  If not,
 * then check if there is a suffix and if THAT is resolvable.
 */
public class Resolver {

    private BcidService bcidService;
    private SettingsManager settingsManager;

    @Autowired
    public Resolver(BcidService bcidService, SettingsManager settingsManager) {
        this.bcidService = bcidService;
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
        Bcid bcid = bcidService.getBcid(identifier.getBcidIdentifier());

        boolean hasWebAddress = (bcid.getWebAddress() != null && !String.valueOf(bcid.getWebAddress()).isEmpty());

        try {
            switch (bcid.getResourceType()) {
                case Expedition.EXPEDITION_RESOURCE_TYPE:
                    if (hasWebAddress)
                        resolution = bcid.getWebAddress();
                    else {
                        // Try and get expeditionForwardingAddress in Mapping.metadata
                        String expeditionForwardingAddress = mapping.getExpeditionForwardingAddress();

                        if (expeditionForwardingAddress != null && !expeditionForwardingAddress.isEmpty()) {
                            resolution = UriComponentsBuilder.fromUriString(expeditionForwardingAddress)
                                    .buildAndExpand(bcid.getIdentifier()).toUri();
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
                            resolution = new URI(bcid.getWebAddress() + identifier.getSuffix());
                        else {
                            String conceptForwardingAddress = mapping.getConceptForwardingAddress();

                            if (conceptForwardingAddress != null && !conceptForwardingAddress.isEmpty()) {
                                resolution = UriComponentsBuilder.fromUriString(conceptForwardingAddress + identifier.getSuffix())
                                        .buildAndExpand(bcid.getIdentifier()).toUri();
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
