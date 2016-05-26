package biocode.fims.service;

import biocode.fims.bcid.*;
import biocode.fims.digester.Entity;
import biocode.fims.digester.Mapping;
import biocode.fims.entities.*;
import biocode.fims.entities.Bcid;
import biocode.fims.fimsExceptions.BadRequestException;
import biocode.fims.fimsExceptions.FimsException;
import biocode.fims.fimsExceptions.ForbiddenRequestException;
import biocode.fims.mappers.EntityToBcidMapper;
import biocode.fims.repositories.ExpeditionRepository;
import biocode.fims.settings.SettingsManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.net.URI;
import java.util.Set;

/**
 * Service class for handling {@link Expedition} persistence
 */
@Service
@Transactional
public class ExpeditionService {

    @PersistenceContext(unitName = "entityManagerFactory")
    private EntityManager entityManager;

    private final ExpeditionRepository expeditionRepository;
    private final ProjectService projectService;
    private final BcidService bcidService;
    private final SettingsManager settingsManager;

    @Autowired
    public ExpeditionService(ExpeditionRepository expeditionRepository, BcidService bcidService,
                             ProjectService projectService, SettingsManager settingsManager) {
        this.expeditionRepository = expeditionRepository;
        this.bcidService = bcidService;
        this.projectService = projectService;
        this.settingsManager = settingsManager;
    }

    public void create(Expedition expedition, int userId, int projectId, URI webAddress, Mapping mapping) {
        Project project = entityManager.getReference(Project.class, projectId);
        User user = entityManager.getReference(User.class, userId);

        expedition.setProject(project);
        expedition.setUser(user);

        if (!projectService.isUserMemberOfProject(user, project))
            throw new ForbiddenRequestException("User ID " + userId + " is not authorized to create expeditions in this project");

        try {
            checkExpeditionCodeValidAndAvailable(expedition.getExpeditionCode(), projectId);
        } catch (FimsException e) {
            throw new BadRequestException(e.getMessage());
        }

        expeditionRepository.save(expedition);
        Bcid bcid = createExpeditionBcid(expedition, webAddress);
        expedition.setExpeditionBcid(bcid);
        createEntityBcids(mapping, expedition.getExpeditionId(), userId);
    }

    public void update(Expedition expedition) {
        expeditionRepository.save(expedition);
    }

    @Transactional(readOnly = true)
    public Expedition getExpedition(String expeditionCode, int projectId) {
        Expedition expedition = expeditionRepository.findByExpeditionCodeAndProjectProjectId(expeditionCode, projectId);
        if (expedition != null)
            expedition.setExpeditionBcid(bcidService.getBcid(
                    expedition.getExpeditionId(),
                    Expedition.EXPEDITION_RESOURCE_TYPE
            ));
        return expedition;
    }

    /**
     * Find the appropriate entity Bcid for this expedition given an conceptAlias.
     *
     * @param expeditionCode defines the BCID expeditionCode to lookup
     * @param conceptAlias the Expedition entity conceptAlias
     *
     * @return returns the BCID for this expedition and conceptURI combination
     */
    @Transactional(readOnly = true)
    public Bcid getEntityBcid(String expeditionCode, int projectId, String conceptAlias) {
        Expedition expedition = getExpedition(expeditionCode, projectId);

        if (expedition == null) {
            throw new EmptyResultDataAccessException(1);
        }

        // conceptAlias is the Bcid title
        return bcidService.getBcidByTitle(
                expedition.getExpeditionId(),
                conceptAlias
        );
    }

    @Transactional(readOnly = true)
    public Page<Expedition> getExpeditions(int projectId, int userId, Pageable pageRequest) {
        Page<Expedition> expeditions = expeditionRepository.findByProjectProjectIdAndProjectUserUserId(projectId, userId, pageRequest);

        for (Expedition expedition: expeditions) {
            expedition.setExpeditionBcid(bcidService.getBcid(
                    expedition.getExpeditionId(),
                    Expedition.EXPEDITION_RESOURCE_TYPE
            ));
        }
        return expeditions;
    }

    @Transactional(readOnly = true)
    public Expedition getExpedition(int expeditionId) {
        Expedition expedition = expeditionRepository.findByExpeditionId(expeditionId);
        if (expedition != null)
            expedition.setExpeditionBcid(bcidService.getBcid(
                    expedition.getExpeditionId(),
                    Expedition.EXPEDITION_RESOURCE_TYPE
            ));
        return expedition;
    }

    public void delete(int expeditionId) {
        Expedition expedition = getExpedition(expeditionId);
        bcidService.delete(expedition.getExpeditionBcid().getBcidId());
        expeditionRepository.deleteByExpeditionId(expeditionId);
    }

    /**
     * set the {@link Bcid} identifier for each {@link Entity} in the {@link Mapping}
     * @param mapping
     * @param expeditionCode
     * @param projectId
     */
    @Transactional(readOnly = true)
    public void setEntityIdentifiers(Mapping mapping, String expeditionCode, int projectId) {
        Expedition expedition = getExpedition(expeditionCode, projectId);
        Set<Bcid> expeditionEntityBcids = bcidService.getEntityBcids(expedition.getExpeditionId());

        for (Bcid bcid: expeditionEntityBcids) {
            for (Entity entity: mapping.getEntities()) {
                if (bcid.getTitle().equals(entity.getConceptAlias())) {
                    entity.setIdentifier(bcid.getIdentifier());
                }
            }
        }

    }

    /**
     * create the Bcid domain object that represents the Expedition
     * @param expedition
     * @param webAddress
     * @return
     */
    private Bcid createExpeditionBcid(Expedition expedition, URI webAddress) {
        boolean ezidRequest = Boolean.parseBoolean(settingsManager.retrieveValue("ezidRequest"));

        Bcid expditionBcid = new Bcid.BcidBuilder(Expedition.EXPEDITION_RESOURCE_TYPE)
                .webAddress(webAddress)
                .title("Expedition: " + expedition.getExpeditionTitle())
                .ezidRequest(ezidRequest)
                .build();

        bcidService.create(expditionBcid, expedition.getUser().getUserId());
        bcidService.attachBcidToExpedition(expditionBcid, expedition.getExpeditionId());
        return expditionBcid;
    }

    private void createEntityBcids(Mapping mapping, int expeditionId, int userId) {
        EntityToBcidMapper mapper = new EntityToBcidMapper();

        for (Entity entity: mapping.getEntities()) {
            Bcid bcid = mapper.map(entity);
            bcidService.create(bcid, userId);
            bcidService.attachBcidToExpedition(bcid, expeditionId);

            entity.setIdentifier(bcid.getIdentifier());
        }
    }

    /**
     * Check that expedition code is between 4 and 50 characters and doesn't already exist in the {@link Project}
     *
     * @return
     */
    private void checkExpeditionCodeValidAndAvailable(String expeditionCode, int projectId) throws FimsException {
        // Check expeditionCode length
        if (expeditionCode.length() < 4 || expeditionCode.length() > 50) {
            throw new FimsException("Expedition code " + expeditionCode + " must be between 4 and 50 characters long");
        }

        // Check to make sure characters are normal!
        if (!expeditionCode.matches("[a-zA-Z0-9_-]*")) {
            throw new FimsException("Expedition code " + expeditionCode + " contains one or more invalid characters. " +
                    "Expedition code characters must be in one of the these ranges: [a-Z][0-9][-][_]");
        }

        if (getExpedition(expeditionCode, projectId) != null)
            throw new FimsException("Expedition Code " + expeditionCode + " already exists.");
    }
}
