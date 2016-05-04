package biocode.fims.service;

import biocode.fims.bcid.ResourceType;
import biocode.fims.bcid.ResourceTypes;
import biocode.fims.entities.*;
import biocode.fims.repositories.ExpeditionRepository;
import biocode.fims.settings.SettingsManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;

/**
 * Service class for handling {@link Expedition} persistence
 */
@Service
public class ExpeditionService {

    private final ExpeditionRepository expeditionRepository;
    private final BcidService bcidService;
    private final SettingsManager settingsManager;

    @Autowired
    public ExpeditionService(ExpeditionRepository expeditionRepository, BcidService bcidService,
                             SettingsManager settingsManager) {
        this.expeditionRepository = expeditionRepository;
        this.bcidService = bcidService;
        this.settingsManager = settingsManager;
    }

    @Transactional
    public void create(Expedition expedition, URI webAddress) {

        expeditionRepository.save(expedition);
        createExpeditionBcid(expedition, webAddress);

    }

    public void update(Expedition expedition) {
        expeditionRepository.save(expedition);
    }

    public Expedition getExpedition(String expeditionCode, int projectId) {
        return expeditionRepository.findByExpeditionCodeAndProjectProjectId(expeditionCode, projectId);
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
        Expedition expedition = getExpedition(expeditionCode, projectId);

        if (expedition == null) {
            throw new EmptyResultDataAccessException(1);
        }

        ResourceTypes resourceTypes = new ResourceTypes();
        ResourceType rt = resourceTypes.getByShortName(conceptAlias);
        String uri = rt.uri;

        return bcidService.getBcid(
                expedition.getExpeditionId(),
                conceptAlias, uri
        );
    }

    public Page<Expedition> getExpeditions(int projectId, int userId, Pageable pageRequest) {
        return expeditionRepository.findByProjectProjectIdAndProjectUserUserId(projectId, userId, pageRequest);
    }

    /**
     * create the Bcid domain object that represets the Expedition
     * @param expedition
     * @param webAddress
     * @return
     */
    private Bcid createExpeditionBcid(Expedition expedition, URI webAddress) {
        boolean ezidRequest = Boolean.parseBoolean(settingsManager.retrieveValue("ezidRequest"));

        Bcid expditionBcid = new Bcid.BcidBuilder(expedition.getUser(), Expedition.EXPEDITION_RESOURCE_TYPE)
                .webAddress(webAddress)
                .title("Expedition: " + expedition.getExpeditionTitle())
                .ezidRequest(ezidRequest)
                .expedition(expedition)
                .build();

        bcidService.create(expditionBcid);
        return expditionBcid;
    }
}