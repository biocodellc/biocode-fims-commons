package biocode.fims.service;

import biocode.fims.digester.Entity;
import biocode.fims.digester.Mapping;
import biocode.fims.entities.*;
import biocode.fims.entities.Bcid;
import biocode.fims.fimsExceptions.BadRequestException;
import biocode.fims.fimsExceptions.FimsException;
import biocode.fims.fimsExceptions.FimsRuntimeException;
import biocode.fims.fimsExceptions.ForbiddenRequestException;
import biocode.fims.fimsExceptions.errorCodes.ProjectCode;
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
import java.util.List;
import java.util.stream.Collectors;

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

        if (!projectService.isUserMemberOfProject(user, project.getProjectId()))
            throw new ForbiddenRequestException("User ID " + userId + " is not authorized to create expeditions in this project");

        try {
            checkExpeditionCodeValidAndAvailable(expedition.getExpeditionCode(), projectId);
        } catch (FimsException e) {
            throw new BadRequestException(e.getMessage());
        }

        expeditionRepository.save(expedition);

        boolean ezidRequest = Boolean.parseBoolean(settingsManager.retrieveValue("ezidRequests"));

        Bcid bcid = createExpeditionBcid(expedition, webAddress, ezidRequest);
        expedition.setExpeditionBcid(bcid);
        createEntityBcids(mapping, expedition.getExpeditionId(), userId, ezidRequest);
    }

    public void update(Expedition expedition) {
        expeditionRepository.save(expedition);
    }

    @Transactional(readOnly = true)
    public Expedition getExpedition(String expeditionCode, int projectId) {
        Expedition expedition = expeditionRepository.findByExpeditionCodeAndProjectProjectId(expeditionCode, projectId);
        if (expedition != null) {
            attachExpeditionBcids(expedition);
        }
        return expedition;
    }

    @Transactional(readOnly = true)
    public Expedition getExpedition(String identifier) {
        Expedition expedition = null;
        Bcid bcid = bcidService.getBcid(identifier);
        if (bcid != null) {
            expedition = bcid.getExpedition();
            expedition.setExpeditionBcid(bcid);
        }
        return expedition;
    }

    /**
     * Find the appropriate entity Bcid for this expedition given an conceptAlias.
     *
     * @param expeditionCode defines the BCID expeditionCode to lookup
     * @param conceptAlias   the Expedition entity conceptAlias
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

        for (Expedition expedition : expeditions) {
            attachExpeditionBcids(expedition);
        }
        return expeditions;
    }

    @Transactional(readOnly = true)
    public List<Expedition> getExpeditionsForUser(int projectId, int userId, boolean includePrivate) {
        List<Expedition> expeditions;

        expeditions = expeditionRepository.getUserProjectExpeditions(projectId, userId, includePrivate);

        for (Expedition expedition : expeditions) {
            attachExpeditionBcids(expedition);
        }

        return expeditions;
    }

    @Transactional(readOnly = true)
    public Expedition getExpedition(int expeditionId) {
        Expedition expedition = expeditionRepository.findByExpeditionId(expeditionId);
        if (expedition != null) {
            attachExpeditionBcids(expedition);
        }
        return expedition;
    }

    public void delete(int expeditionId) {
        expeditionRepository.deleteByExpeditionId(expeditionId);
    }

    public void delete(String expeditionCode, int projectId) {
        expeditionRepository.deleteByExpeditionCodeAndProjectProjectId(expeditionCode, projectId);
    }

    /**
     * set the {@link Bcid} identifier for each {@link Entity} in the {@link Mapping}
     *
     * @param mapping
     * @param expeditionCode
     * @param projectId
     */
    @Transactional(readOnly = true)
    public void setEntityIdentifiers(Mapping mapping, String expeditionCode, int projectId) {
        Expedition expedition = getExpedition(expeditionCode, projectId);
        List<Bcid> expeditionEntityBcids = bcidService.getEntityBcids(expedition.getExpeditionId());

        for (Bcid bcid : expeditionEntityBcids) {
            for (Entity entity : mapping.getEntities()) {
                if (bcid.getTitle().equals(entity.getConceptAlias())) {
                    entity.setIdentifier(bcid.getIdentifier());
                }
            }
        }

    }

    /**
     * create the Bcid domain object that represents the Expedition
     *
     * @param expedition
     * @param webAddress
     * @return
     */
    private Bcid createExpeditionBcid(Expedition expedition, URI webAddress, boolean ezidRequest) {

        Bcid expditionBcid = new Bcid.BcidBuilder(Expedition.EXPEDITION_RESOURCE_TYPE)
                .webAddress(webAddress)
                .title("Expedition: " + expedition.getExpeditionTitle())
                .ezidRequest(ezidRequest)
                .build();

        bcidService.create(expditionBcid, expedition.getUser().getUserId());
        bcidService.attachBcidToExpedition(expditionBcid, expedition.getExpeditionId());
        return expditionBcid;
    }

    private void createEntityBcids(Mapping mapping, int expeditionId, int userId, boolean ezidRequest) {
        for (Entity entity : mapping.getEntities()) {
            Bcid bcid = EntityToBcidMapper.map(entity, ezidRequest);
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
        if (expeditionCode == null || expeditionCode.length() < 4 || expeditionCode.length() > 50) {
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

    /**
     * attach the expedition bcid and expedition entity bcids to the {@link Expedition} object
     *
     * @param expedition
     */
    private void attachExpeditionBcids(Expedition expedition) {
        expedition.setExpeditionBcid(
                bcidService.getBcid(
                        expedition.getExpeditionId(),
                        Expedition.EXPEDITION_RESOURCE_TYPE
                ));
        expedition.setEntityBcids(bcidService.getEntityBcids(expedition.getExpeditionId()));
    }

    public List<Expedition> getPublicExpeditions(int projectId) {
        return expeditionRepository.findByPublicTrueAndProjectProjectId(projectId);
    }

    /**
     * bulk update expeditions for a project
     *
     * @param expeditions
     */
    public void update(List<Expedition> expeditions, int projectId) {
        if (!expeditionsBelongToProject(expeditions, projectId)) {
            throw new FimsRuntimeException(ProjectCode.INVALID_EXPEDITION, 400);
        }

        expeditionRepository.save(expeditions);
    }


    /**
     * check that all the expeditions belong to the project
     *
     * @param expeditions
     * @param projectId
     */
    private boolean expeditionsBelongToProject(List<Expedition> expeditions, int projectId) {
        List<Integer> expeditionIds = expeditions
                .stream()
                .map(Expedition::getExpeditionId)
                .collect(Collectors.toList());

        return expeditionIds.size() == expeditionRepository.countByExpeditionIdInAndProjectProjectId(expeditionIds, projectId);

    }
}
