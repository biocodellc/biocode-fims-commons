package biocode.fims.service;

import biocode.fims.application.config.FimsProperties;
import biocode.fims.bcid.*;
import biocode.fims.entities.BcidTmp;
import biocode.fims.entities.*;
import biocode.fims.repositories.BcidRepository;
import biocode.fims.repositories.BcidTmpRepository;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.util.*;

/**
 * Service class for handling {@link BcidTmp} persistence
 */
@Service
public class BcidService {

    @PersistenceContext(unitName = "entityManagerFactory")
    private EntityManager entityManager;

    private final BcidRepository bcidRepository;
    private final BcidTmpRepository bcidTmpRepository;
    protected final FimsProperties props;
    private final UserService userService;

    @Autowired
    public BcidService(BcidRepository bcidRepository, BcidTmpRepository bcidTmpRepository, FimsProperties props,
                       UserService userService) {
        this.bcidRepository = bcidRepository;
        this.bcidTmpRepository = bcidTmpRepository;
        this.props = props;
        this.userService = userService;
    }

    @Transactional
    public BcidTmp create(BcidTmp bcidTmp, int userId) {

        User user = userService.getUser(userId);
        bcidTmp.setUser(user);

        // if the user is demo, never create ezid's
        if (bcidTmp.isEzidRequest() && userService.getUser(userId).getUsername().equals("demo"))
            bcidTmp.setEzidRequest(false);

        String creator = (StringUtils.isEmpty(props.creator())) ? user.getFullName() + " <" + user.getEmail() + ">" : props.creator();
        Bcid bcid = Bcid.fromBcidTmp(bcidTmp, creator, props.publisher());

        bcid = bcidRepository.create(bcid);

        bcidTmp.setIdentifier(bcid.identifier());
        bcidTmpRepository.save(bcidTmp);

        return bcidTmp;

    }

    public BcidTmp attachBcidToExpedition(BcidTmp bcidTmp, int expeditionId) {
        Expedition expedition = entityManager.getReference(Expedition.class, expeditionId);
        bcidTmp.setExpedition(expedition);

        update(bcidTmp);

        return bcidTmp;
    }

    public void update(BcidTmp bcidTmp) {
        bcidTmpRepository.save(bcidTmp);
    }

    @Transactional(readOnly = true)
    public BcidTmp getBcid(String identifier) {
        return bcidTmpRepository.findByIdentifier(identifier);
    }

    @Transactional(readOnly = true)
    public List<BcidTmp> getBcids(List<String> graph) {
        return bcidTmpRepository.findAllByGraphIn(graph);
    }

    @Transactional(readOnly = true)
    public BcidTmp getBcidByTitle(int expeditionId, String title) {
        return bcidTmpRepository.findOneByTitleAndExpeditionExpeditionId(title, expeditionId);
    }

    /**
     * @param expeditionId the {@link biocode.fims.entities.Expedition} the bcids are associated with
     * @param resourceType the resourceType(s) of the Bcids to find
     * @return the {@link BcidTmp} associated with the provided {@link biocode.fims.entities.Expedition}, containing
     * the provided resourceType(s)
     */
    @Transactional(readOnly = true)
    public BcidTmp getBcid(int expeditionId, String... resourceType) {
        return bcidTmpRepository.findByExpeditionExpeditionIdAndResourceTypeIn(expeditionId, resourceType);
    }


    @Transactional(readOnly = true)
    public List<BcidTmp> getDatasets(int projectId, String expeditionCode) {
        return bcidTmpRepository.findAllByResourceType(
                projectId,
                expeditionCode,
                ResourceTypes.DATASET_RESOURCE_TYPE
        );
    }

    public void delete(int bcidId) {
        bcidTmpRepository.deleteByBcidId(bcidId);
    }

    /**
     * Entity Bcids are all Bcids with resourceType != Dataset or Expedition resourceType
     *
     * @param expeditionId
     * @return
     */
    @Transactional(readOnly = true)
    public List<BcidTmp> getEntityBcids(int expeditionId) {
        return bcidTmpRepository.findByExpeditionExpeditionIdAndResourceTypeNotIn(expeditionId,
                ResourceTypes.DATASET_RESOURCE_TYPE, Expedition.EXPEDITION_RESOURCE_TYPE);
    }

    @Transactional(readOnly = true)
    public Set<BcidTmp> getBcidsWithEzidRequest() {
        return bcidTmpRepository.findAllByEzidRequestTrue();
    }


    public List<BcidTmp> getBcidsWithOutEzidRequest() {
        return bcidTmpRepository.findAllByEzidRequestFalse();
    }
}
