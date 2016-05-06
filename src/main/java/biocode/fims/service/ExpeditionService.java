package biocode.fims.service;

import biocode.fims.bcid.ResourceType;
import biocode.fims.bcid.ResourceTypes;
import biocode.fims.entities.*;
import biocode.fims.fimsExceptions.BadRequestException;
import biocode.fims.fimsExceptions.FimsException;
import biocode.fims.fimsExceptions.ForbiddenRequestException;
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
        boolean userIsProjectMember = false;
        for (User member: expedition.getProject().getProjectMembers()) {
            if (member.equals(expedition.getUser()))
                userIsProjectMember = true;
        }
        if (!userIsProjectMember)
            throw new ForbiddenRequestException("User ID " + expedition.getUser().getUserId() + " is not authorized to create expeditions in this project");

        try {
            checkExpeditionCodeValidAndAvailable(expedition);
        } catch (FimsException e) {
            throw new BadRequestException(e.getMessage());
        }

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

    /**
     * Check that expedition code is between 4 and 50 characters and doesn't already exist in the {@link Project}
     *
     * @param expedition
     *
     * @return
     */
    private void checkExpeditionCodeValidAndAvailable(Expedition expedition) throws FimsException {
        String expeditionCode = expedition.getExpeditionCode();
        // Check expeditionCode length
        if (expeditionCode.length() < 4 || expeditionCode.length() > 50) {
            throw new FimsException("Expedition code " + expeditionCode + " must be between 4 and 50 characters long");
        }

        // Check to make sure characters are normal!
        if (!expeditionCode.matches("[a-zA-Z0-9_-]*")) {
            throw new FimsException("Expedition code " + expeditionCode + " contains one or more invalid characters. " +
                    "Expedition code characters must be in one of the these ranges: [a-Z][0-9][-][_]");
        }

        if (getExpedition(expeditionCode, expedition.getProject().getProjectId()) != null)
            throw new FimsException("Expedition Code " + expeditionCode + " already exists.");
    }
}
