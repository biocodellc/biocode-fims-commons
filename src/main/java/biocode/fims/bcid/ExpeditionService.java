package biocode.fims.bcid;

import biocode.fims.entities.Bcid;
import biocode.fims.entities.Expedition;
import biocode.fims.repository.BcidRepository;
import biocode.fims.repository.ExpeditionRepository;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Class which implements the business logic regarding {@link Expedition}
 */
public class ExpeditionService {

    private ExpeditionRepository expeditionRepository;
    private BcidRepository bcidRepository;

    @Autowired
    public ExpeditionService(ExpeditionRepository expeditionRepository, BcidRepository bcidRepository) {
        this.expeditionRepository = expeditionRepository;
        this.bcidRepository = bcidRepository;
    }

    /**
     * Find the appropriate root BCID for this expedition given an conceptAlias.
     *
     * @param expeditionCode defines the BCID expeditionCode to lookup
     * @param conceptAlias    defines the alias to narrow this,  a one-word reference denoting a BCID
     *
     * @return returns the BCID for this expedition and conceptURI combination
     */
    public Bcid getRootBcid(String expeditionCode, int projectId, String conceptAlias) {
        Expedition expedition = expeditionRepository.findByExpeditionCodeAndProjectId(expeditionCode, projectId);

        ResourceTypes resourceTypes = new ResourceTypes();
        ResourceType rt = resourceTypes.getByShortName(conceptAlias);
        String uri = rt.uri;

        return bcidRepository.findByExpeditionAndResourceType(
                expedition.getExpeditionId(),
                conceptAlias, uri
        ).iterator().next();
    }
}
