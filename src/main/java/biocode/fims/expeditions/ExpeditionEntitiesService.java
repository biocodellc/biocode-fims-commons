package biocode.fims.expeditions;

import biocode.fims.entities.Bcid;
import biocode.fims.bcid.BcidMinter;
import biocode.fims.bcid.ExpeditionMinter;
import biocode.fims.digester.Entity;
import biocode.fims.entities.Expedition;
import biocode.fims.run.ProcessController;
import biocode.fims.service.BcidService;
import biocode.fims.service.ExpeditionService;
import biocode.fims.settings.FimsPrinter;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Created by rjewing on 5/20/16.
 */
public class ExpeditionEntitiesService {
    private static String DEFAULT_IDENTIFIER_PREFIX = "urn:x-biscicol:";
    private final ExpeditionEntities expeditionEntities;
    private final ProcessController processController;
    private final BcidService bcidService;
    private final ExpeditionService expeditionService;

    public ExpeditionEntitiesService(ExpeditionEntities expeditionEntities, ProcessController processController,
                                     BcidService bcidService, ExpeditionService expeditionService) {
        this.expeditionEntities = expeditionEntities;
        this.processController = processController;
        this.bcidService = bcidService;
        this.expeditionService = expeditionService;
    }

    public String getEntityIdentifier(Entity entity) {
        // Use the DeepRoots System to lookup Key
        String identifier = null;
        if (expeditionEntities != null) {
            identifier = lookupPrefix(entity);
        }

        // Use the default namespace value if dRoots is unsuccessful...
        if (identifier == null) {
            identifier = DEFAULT_IDENTIFIER_PREFIX + entity.getConceptAlias() + ":";
        }
        return identifier;
    }
        /**
         * Find the appropriate identifier for a concept contained in this file
         *
         * @return returns the Bcid for Entity resourceType in this DeepRoots file
         */
    public String lookupPrefix(Entity entity) {
        HashMap<String, String> entities = expeditionEntities.getEntities();
        Iterator it = entities.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry pairs = (Map.Entry) it.next();
            if (pairs.getKey().toString().trim().equals(entity.getConceptAlias().trim())) {
                String postfix = (String) pairs.getValue();
                return postfix;
            }
        }
        FimsPrinter.out.println("\tWarning: " + entity.getConceptAlias() + " cannot be mapped in Deep Roots, attempting to create mapping");
        // Create a mapping in the deeproots system for this URI
        FimsPrinter.out.println("\tCreating bcid root for " + entity.getConceptAlias() + " with resource type = " + entity.getConceptURI());

        Bcid bcid = new Bcid.BcidBuilder(entity.getConceptAlias())
                .title(entity.getConceptAlias())
                .build();

        bcidService.create(bcid, processController.getUserId());

        Expedition expedition = expeditionService.getExpedition(processController.getExpeditionCode(), processController.getProjectId());
        bcidService.attachBcidToExpedition(bcid, expedition.getExpeditionId());

        // Add this element to the entities string so we don't keep trying to add it in the loop above
        entities.put(entity.getConceptAlias(), String.valueOf(bcid.getIdentifier()));
        System.out.println("\tNew identifier = " + bcid.getIdentifier());
        return String.valueOf(bcid.getIdentifier());
    }
}
