package biocode.fims.bcid;

import biocode.fims.digester.Mapping;
import biocode.fims.entities.BcidTmp;
import biocode.fims.entities.Expedition;
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
        BcidTmp bcidTmp = bcidService.getBcid(identifier.getBcidIdentifier());

        boolean hasWebAddress = (bcidTmp.getWebAddress() != null && !String.valueOf(bcidTmp.getWebAddress()).isEmpty());

        try {
            switch (bcidTmp.getResourceType()) {
                case Expedition.EXPEDITION_RESOURCE_TYPE:
                    if (hasWebAddress)
                        resolution = bcidTmp.getWebAddress();
                    else if (mapping != null) {
                        // Try and get expeditionForwardingAddress in Mapping.metadata
                        String expeditionForwardingAddress = mapping.getMetadata().getExpeditionForwardingAddress();

                        if (!StringUtils.isEmpty(expeditionForwardingAddress)) {
                            resolution = UriComponentsBuilder.fromUriString(expeditionForwardingAddress)
                                    .buildAndExpand(bcidTmp.getIdentifier()).toUri();
                        }
                    }
                    break;
                case ResourceTypes.DATASET_RESOURCE_TYPE:
                    if (hasWebAddress) {
                        resolution = bcidTmp.getWebAddress();
                    } else if (mapping != null) {
                        // Try and get datasetForwardingAddress in Mapping.metadata
                        String datasetForwardingAddress = mapping.getMetadata().getDatasetForwardingAddress();

                        if (!StringUtils.isEmpty(datasetForwardingAddress)) {
                            resolution = UriComponentsBuilder.fromUriString(datasetForwardingAddress)
                                    .buildAndExpand(bcidTmp.getIdentifier()).toUri();
                        }
                    }
                    break;
                default:
                    if (identifier.hasSuffix()) {
                        if (hasWebAddress)
                            resolution = new URI(bcidTmp.getWebAddress() + identifier.getSuffix());
                        else {
                            if (bcidTmp.getExpedition() != null) {
                                expeditionService.setEntityIdentifiers(mapping, bcidTmp.getExpedition().getExpeditionCode(),
                                        bcidTmp.getExpedition().getProject().getProjectId());
                                String conceptForwardingAddress = mapping.getConceptForwardingAddress(String.valueOf(bcidTmp.getIdentifier()));

                                if (!StringUtils.isEmpty(conceptForwardingAddress)) {
                                    Map<String, String> urlMap = new HashMap();
                                    urlMap.put("ark", String.valueOf(bcidTmp.getIdentifier()));
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
                    resolverMetadataPrefix + "\" and bcid: \"" + bcidTmp + "\"", e);
        }

        return resolution;
    }
}
