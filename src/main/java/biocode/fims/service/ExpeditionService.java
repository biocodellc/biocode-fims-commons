package biocode.fims.service;

import biocode.fims.authorizers.ProjectAuthorizer;
import biocode.fims.application.config.FimsProperties;
import biocode.fims.bcid.Bcid;
import biocode.fims.config.models.Entity;
import biocode.fims.config.project.ProjectConfig;
import biocode.fims.fimsExceptions.errorCodes.ExpeditionCode;
import biocode.fims.models.*;
import biocode.fims.fimsExceptions.BadRequestException;
import biocode.fims.fimsExceptions.FimsException;
import biocode.fims.fimsExceptions.FimsRuntimeException;
import biocode.fims.fimsExceptions.ForbiddenRequestException;
import biocode.fims.fimsExceptions.errorCodes.ProjectCode;
import biocode.fims.repositories.ExpeditionRepository;
import com.sun.org.apache.xpath.internal.operations.Bool;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
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
    private final BcidService bcidService;
    private final ProjectAuthorizer projectAuthorizer;
    private final FimsProperties props;

    @Autowired
    public ExpeditionService(ExpeditionRepository expeditionRepository, BcidService bcidService,
                             ProjectAuthorizer projectAuthorizer, FimsProperties props) {
        this.expeditionRepository = expeditionRepository;
        this.bcidService = bcidService;
        this.projectAuthorizer = projectAuthorizer;
        this.props = props;
    }

    public Expedition create(Expedition expedition, int userId, int projectId, URI webAddress) {
        Project project = entityManager.getReference(Project.class, projectId);
        User user = entityManager.getReference(User.class, userId);

        expedition.setProject(project);
        expedition.setUser(user);

        if (!projectAuthorizer.userHasAccess(user, project)) {
            throw new ForbiddenRequestException("User ID " + userId + " is not authorized to create expeditions in this project");
        }

        checkExpeditionMetadata(expedition);
        try {
            checkExpeditionCodeValidAndAvailable(expedition.getExpeditionCode(), projectId);
        } catch (FimsException e) {
            throw new BadRequestException(e.getMessage());
        }

        Bcid bcid = createExpeditionBcid(expedition, webAddress, expedition.getUser());
        expedition.setIdentifier(bcid.identifier());
        expeditionRepository.save(expedition);

        List<EntityIdentifier> entityIdentifiers = createEntityBcids(expedition, project.getProjectConfig().entities(), expedition.getUser());
        expedition.setEntityIdentifiers(entityIdentifiers);
        return expeditionRepository.save(expedition);
    }

    public void update(Expedition expedition) {
        expeditionRepository.save(expedition);
    }

    @Transactional(readOnly = true)
    public Expedition getExpedition(String expeditionCode, int projectId) {
        return expeditionRepository.findByExpeditionCodeAndProjectProjectId(expeditionCode, projectId);
    }

    @Transactional(readOnly = true)
    public Expedition getExpedition(String identifier) {
        try {
            return expeditionRepository.findByIdentifier(new URI(identifier));
        } catch (URISyntaxException e) {
            throw new BadRequestException("Malformed identifier");
        }
    }

    @Transactional(readOnly = true)
    public Page<Expedition> getExpeditions(int projectId, int userId, Pageable pageRequest) {
        return expeditionRepository.findByProjectProjectIdAndProjectUserUserId(projectId, userId, pageRequest);
    }

    @Transactional(readOnly = true)
    public List<Expedition> getExpeditionsForUser(int projectId, int userId, boolean includePrivate) {
        return expeditionRepository.getUserProjectExpeditions(projectId, userId, includePrivate);
    }

    @Transactional(readOnly = true)
    public List<Expedition> getExpeditions(int projectId, boolean includePrivate) {
        return expeditionRepository.getProjectExpeditions(projectId, includePrivate);
    }

    @Transactional(readOnly = true)
    public Expedition getExpedition(int expeditionId) {
        return expeditionRepository.findByExpeditionId(expeditionId);
    }

    public void delete(int expeditionId) {
        expeditionRepository.deleteByExpeditionId(expeditionId);
    }

    public void delete(String expeditionCode, int projectId) {
        expeditionRepository.deleteByExpeditionCodeAndProjectProjectId(expeditionCode, projectId);
    }

    /**
     * create the Bcid domain object that represents the Expedition
     *
     * @param expedition
     * @param webAddress
     * @return
     */
    private Bcid createExpeditionBcid(Expedition expedition, URI webAddress, User user) {

        Bcid bcid = new Bcid.BcidBuilder(Expedition.EXPEDITION_RESOURCE_TYPE, props.publisher())
                .webAddress(webAddress)
                .title("Expedition: " + expedition.getExpeditionTitle())
                .ezidRequest(props.ezidRequests())
                .creator(user, props.creator())
                .build();

        return bcidService.create(bcid, user);
    }

    public List<EntityIdentifier> createEntityBcids(Expedition expedition, List<Entity> entities, User user) {
        List<EntityIdentifier> identifiers = new ArrayList<>();

        for (Entity entity : entities) {
            Bcid bcid = new Bcid.BcidBuilder(entity.getConceptURI(), props.publisher())
                    .creator(user, props.creator())
                    .title(entity.getConceptAlias())
                    .webAddress(props.entityResolverTarget())
                    .build();

            bcid = bcidService.create(bcid, user);

            identifiers.add(new EntityIdentifier(expedition, entity.getConceptAlias(), bcid.identifier()));
        }

        return identifiers;
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
     * Check that any required metadata properties are present
     *
     * @param expedition
     */
    private void checkExpeditionMetadata(Expedition expedition) {
        ProjectConfig config = expedition.getProject().getProjectConfig();

        Map<String, Object> metadata = expedition.getMetadata();

        List<String> missingMetadata = new ArrayList<>();
        for (ExpeditionMetadataProperty p : config.expeditionMetadataProperties()) {
            if (p.isRequired() && !metadata.containsKey(p.getName())) {
                missingMetadata.add(p.getName());
            }

            Object value = metadata.get(p.getName());
            if (value != null) {
                switch (p.getType()) {
                    case BOOLEAN:
                        if (!isBooleanValue(value)) {
                            throw new FimsRuntimeException(ExpeditionCode.INVALID_METADATA, 400);
                        }
                        metadata.put(p.getName(), Boolean.parseBoolean(String.valueOf(value)));
                        break;
                    case LIST:
                        if (!p.getValues().contains(String.valueOf(value))) {
                            throw new FimsRuntimeException(ExpeditionCode.INVALID_METADATA, 400);
                        }
                        break;
                    case STRING:
                        break;
                }
            }
        }

        if (missingMetadata.size() > 0) {
            throw new FimsRuntimeException(ExpeditionCode.MISSING_METADATA, 400, String.join(", ", missingMetadata));
        }
    }

    private boolean isBooleanValue(Object val) {
        if (val == null) return false;

        String v = String.valueOf(val).toLowerCase();

        return v.equals("true") || v.equals("false");
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
