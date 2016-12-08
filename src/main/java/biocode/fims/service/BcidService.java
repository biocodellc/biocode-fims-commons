package biocode.fims.service;

import biocode.fims.bcid.*;
import biocode.fims.entities.Bcid;
import biocode.fims.ezid.EzidException;
import biocode.fims.ezid.EzidService;
import biocode.fims.ezid.EzidUtils;
import biocode.fims.fileManagers.fimsMetadata.FimsMetadataFileManager;
import biocode.fims.fimsExceptions.ServerErrorException;
import biocode.fims.entities.*;
import biocode.fims.repositories.BcidRepository;
import biocode.fims.settings.SettingsManager;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.math.BigInteger;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;

/**
 * Service class for handling {@link Bcid} persistence
 */
@Service
public class BcidService {
    private static final String scheme = "ark:";
    private static final Logger logger = LoggerFactory.getLogger(BcidService.class);

    @PersistenceContext(unitName = "entityManagerFactory")
    EntityManager entityManager;

    private final BcidRepository bcidRepository;
    protected final SettingsManager settingsManager;
    private final UserService userService;
    private final EzidUtils ezidUtils;
    private final BcidEncoder bcidEncoder = new BcidEncoder();

    @Autowired
    public BcidService(BcidRepository bcidRepository, SettingsManager settingsManager,
                       UserService userService, EzidUtils ezidUtils) {
        this.bcidRepository = bcidRepository;
        this.settingsManager = settingsManager;
        this.userService = userService;
        this.ezidUtils = ezidUtils;
    }

    @Transactional
    public Bcid create(Bcid bcid, int userId) {
        int naan = new Integer(settingsManager.retrieveValue("naan"));

        User user = userService.getUser(userId);
        bcid.setUser(user);

        // if the user is demo, never create ezid's
        if (bcid.isEzidRequest() && userService.getUser(userId).getUsername().equals("demo"))
            bcid.setEzidRequest(false);
        bcidRepository.save(bcid);

        // generate the identifier
        try {
            bcid.setIdentifier(generateBcidIdentifier(bcid.getBcidId(), naan));
        } catch (URISyntaxException e) {
            throw new ServerErrorException("Server Error", String.format(
                    "URISyntaxException while generating identifier for bcid: %s", bcid),
                    e);
        }
        bcidRepository.save(bcid);

        if (bcid.isEzidRequest()) {
            createBcidsEZIDs();
        }

        return bcid;

    }

    public Bcid attachBcidToExpedition(Bcid bcid, int expeditionId) {
        Expedition expedition = entityManager.getReference(Expedition.class, expeditionId);
        bcid.setExpedition(expedition);

        update(bcid);

        return bcid;
    }

    public void update(Bcid bcid) {
        bcidRepository.save(bcid);
    }

    @Transactional(readOnly = true)
    public Bcid getBcid(String identifier) {
        return bcidRepository.findByIdentifier(identifier);
    }

    @Transactional(readOnly = true)
    public Bcid getBcid(int bcidId) {
        return bcidRepository.findByBcidId(bcidId);
    }

    @Transactional(readOnly = true)
    public List<Bcid> getBcids(List<String> graph) {
        return bcidRepository.findAllByGraphIn(graph);
    }

    @Transactional(readOnly = true)
    public Bcid getBcidByTitle(int expeditionId, String title) {
        return bcidRepository.findOneByTitleAndExpeditionExpeditionId(title, expeditionId);
    }

    /**
     * @param expeditionId the {@link biocode.fims.entities.Expedition} the bcids are associated with
     * @param resourceType the resourceType(s) of the Bcids to find
     * @return the {@link Bcid} associated with the provided {@link biocode.fims.entities.Expedition}, containing
     * the provided resourceType(s)
     */
    @Transactional(readOnly = true)
    public Bcid getBcid(int expeditionId, String... resourceType) {
        return bcidRepository.findByExpeditionExpeditionIdAndResourceTypeIn(expeditionId, resourceType);
    }

    @Transactional(readOnly = true)
    public Set<Bcid> getLatestDatasets(int projectId) {
        return bcidRepository.findLatestFimsMetadataDatasets(projectId);
    }

    @Transactional
    public void updateTs(Bcid bcid) {
        if (bcid != null) {
            bcidRepository.updateTs(bcid.getBcidId());
        }
    }

    @Transactional(readOnly = true)
    public List<Bcid> getFimsMetadataDatasets(int projectId, String expeditionCode) {
        return bcidRepository.findAllByResourceTypeAndSubResourceType(
                projectId,
                expeditionCode,
                ResourceTypes.DATASET_RESOURCE_TYPE,
                FimsMetadataFileManager.DATASET_RESOURCE_SUB_TYPE
        );
    }

    public void delete(int bcidId) {
        bcidRepository.deleteByBcidId(bcidId);
    }

    /**
     * Entity Bcids are all Bcids with resourceType != Dataset or Expedition resourceType
     *
     * @param expeditionId
     * @return
     */
    @Transactional(readOnly = true)
    public List<Bcid> getEntityBcids(int expeditionId) {
        return bcidRepository.findByExpeditionExpeditionIdAndResourceTypeNotIn(expeditionId,
                ResourceTypes.DATASET_RESOURCE_TYPE, Expedition.EXPEDITION_RESOURCE_TYPE);
    }

    @Transactional(readOnly = true)
    public Set<Bcid> getBcidsWithEzidRequest() {
        return bcidRepository.findAllByEzidRequestTrue();
    }


    private URI generateBcidIdentifier(int bcidId, int naan) throws URISyntaxException {
        String bow = scheme + "/" + naan + "/";

        // Create the shoulder Bcid (String Bcid Bcid)
        String shoulder = bcidEncoder.encode(new BigInteger(String.valueOf(bcidId)));

        // Create the identifier
        return new URI(bow + shoulder);
    }

    @Transactional(readOnly = true)
    private Set<Bcid> getBcidsWithEzidRequestNotMade() {
        return bcidRepository.findAllByEzidRequestTrueAndEzidMadeFalse();
    }

    /**
     * Go through bcids table and create any ezidService fields that have yet to be created. We want to create any
     * EZIDs that have not been made yet.
     * <p/>
     * case
     */
    private void createBcidsEZIDs() {
        // NOTE: On any type of EZID error, we DON'T want to fail the process.. This means we need
        // a separate mechanism on the server side to check creation of EZIDs.  This is easy enough to do
        // in the Database.
        EzidService ezidService = new EzidService();
        HashMap<String, String> ezidErrors = new HashMap<>();
        // Setup EZID account/login information
        try {
            ezidService.login(settingsManager.retrieveValue("eziduser"), settingsManager.retrieveValue("ezidpass"));
        } catch (EzidException e) {
            ezidErrors.put(null, ExceptionUtils.getStackTrace(e));

        }
        Set<Bcid> bcids = getBcidsWithEzidRequestNotMade();

        for (Bcid bcid : bcids) {
            // Dublin Core metadata profile element
            HashMap<String, String> map = ezidUtils.getDcMap(bcid);

            // Register this as an EZID
            try {
                URI identifier = new URI(ezidService.createIdentifier(String.valueOf(bcid.getIdentifier()), map));
                bcid.setEzidMade(true);
                logger.info("{}", identifier.toString());
            } catch (EzidException e) {
                logger.info("EzidException thrown trying to create Ezid {}. Trying to update now.", bcid.getIdentifier(), e);
                // Attempt to set Metadata if this is an Exception
                try {
                    ezidService.setMetadata(String.valueOf(bcid.getIdentifier()), map);
                    bcid.setEzidMade(true);
                } catch (EzidException e1) {
                    logger.error("Exception thrown in attempting to create OR update EZID {}, a permission issue?", bcid.getIdentifier(), e1);
                    ezidErrors.put(String.valueOf(bcid.getIdentifier()), ExceptionUtils.getStackTrace(e1));
                }

            } catch (URISyntaxException e) {
                logger.error("Bad uri syntax for " + bcid.getIdentifier() + ", " + map, e);
                ezidErrors.put(String.valueOf(bcid.getIdentifier()), "Bad uri syntax");
            }
        }

        if (!ezidErrors.isEmpty()) {
            ezidUtils.sendErrorEmail(ezidErrors);
        }
    }
}
