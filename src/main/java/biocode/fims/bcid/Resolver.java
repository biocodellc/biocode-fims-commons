package biocode.fims.bcid;

import biocode.fims.digester.Mapping;
import biocode.fims.models.Bcid;
import biocode.fims.models.Expedition;
import biocode.fims.fimsExceptions.ServerErrorException;
import biocode.fims.service.BcidService;
import biocode.fims.service.ExpeditionService;
import biocode.fims.settings.SettingsManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;

/**
 * Resolves any incoming Bcid to the BCID and/or EZID systems.
 * Resolver first checks if this is a data group.  If so, it then checks if there is a decodable BCID.  If not,
 * then check if there is a suffix and if THAT is resolvable.
 */
public class Resolver {

    private BcidService bcidService;
    private SettingsManager settingsManager;
    private ExpeditionService expeditionService;

    @Autowired
    public Resolver(BcidService bcidService, SettingsManager settingsManager, ExpeditionService expeditionService) {
        this.bcidService = bcidService;
        this.settingsManager = settingsManager;
        this.expeditionService = expeditionService;
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
                    else if (mapping != null) {
                        // Try and get expeditionForwardingAddress in Mapping.metadata
                        String expeditionForwardingAddress = mapping.getMetadata().getExpeditionForwardingAddress();

                        if (!StringUtils.isEmpty(expeditionForwardingAddress)) {
                            resolution = UriComponentsBuilder.fromUriString(expeditionForwardingAddress)
                                    .buildAndExpand(bcid.getIdentifier()).toUri();
                        }
                    }
                    break;
                case ResourceTypes.DATASET_RESOURCE_TYPE:
                    if (hasWebAddress) {
                        resolution = bcid.getWebAddress();
                    } else if (mapping != null) {
                        // Try and get datasetForwardingAddress in Mapping.metadata
                        String datasetForwardingAddress = mapping.getMetadata().getDatasetForwardingAddress();

                        if (!StringUtils.isEmpty(datasetForwardingAddress)) {
                            resolution = UriComponentsBuilder.fromUriString(datasetForwardingAddress)
                                    .buildAndExpand(bcid.getIdentifier()).toUri();
                        }
                    }
                    break;
                default:
                    if (identifier.hasSuffix()) {
                        if (hasWebAddress)
                            resolution = new URI(bcid.getWebAddress() + identifier.getSuffix());
                        else {
                            if (bcid.getExpedition() != null) {
                                expeditionService.setEntityIdentifiers(mapping, bcid.getExpedition().getExpeditionCode(),
                                        bcid.getExpedition().getProject().getProjectId());
                                String conceptForwardingAddress = mapping.getConceptForwardingAddress(String.valueOf(bcid.getIdentifier()));

                                if (!StringUtils.isEmpty(conceptForwardingAddress)) {
                                    Map<String, String> urlMap = new HashMap();
                                    urlMap.put("ark", String.valueOf(bcid.getIdentifier()));
                                    urlMap.put("suffix", String.valueOf(identifier.getSuffix()));
                                    resolution = UriComponentsBuilder.fromUriString(conceptForwardingAddress)
                                            .buildAndExpand(urlMap).toUri();
                                }
                            }
                        }
                    }

                    break;
            }
            // if resolution is still null, then resolve to the default metadata service
            if (StringUtils.isEmpty(resolution)) {
                resolution = new URI(resolverMetadataPrefix + identifier.getIdentifier());
            }
        } catch(URISyntaxException e) {
            throw new ServerErrorException("Server Error", "Syntax exception thrown for metadataTargetPrefix: \"" +
                    resolverMetadataPrefix + "\" and bcid: \"" + bcid + "\"", e);
        }

        return resolution;
    }
}
