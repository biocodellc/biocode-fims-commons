package biocode.fims.service;

import biocode.fims.bcid.*;
import biocode.fims.entities.Bcid;
import biocode.fims.ezid.EzidException;
import biocode.fims.ezid.EzidService;
import biocode.fims.fimsExceptions.ServerErrorException;
import biocode.fims.entities.*;
import biocode.fims.repositories.BcidRepository;
import biocode.fims.repositories.ExpeditionRepository;
import biocode.fims.repositories.UserRepository;
import biocode.fims.settings.SettingsManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.math.BigInteger;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

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
    private final SettingsManager settingsManager;
    private final UserService userService;
    private final BcidEncoder bcidEncoder = new BcidEncoder();

    @Autowired
    public BcidService(BcidRepository bcidRepository, SettingsManager settingsManager,
                       UserService userService) {
        this.bcidRepository = bcidRepository;
        this.settingsManager = settingsManager;
        this.userService = userService;
    }

    public Bcid create(Bcid bcid, int userId) {
        transactionalCreate(bcid, userId);

        if (bcid.isEzidRequest()) {
            createEzid(bcid);
        }

        return bcid;

    }

    /**
     * This method wraps the process of creating a Bcid, minus the corresponding Ezid in a {@link Transactional}.
     * If we include the call to createEzid in a {@link Transactional}, the Ezid will not be created for the current
     * Bcid as the changes are flushed to the db after the {@link Transactional} method is complete
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    private void transactionalCreate(Bcid bcid, int userId) {
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
        return;
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
        return bcidRepository.findLatestDatasets(projectId);
    }

    /**
     * fetch the latest Bcids with resourceType = 'http://purl.org/dc/dcmitype/Dataset' for the provided list of
     * {@link Expedition}s
     * @param expeditions
     * @return
     */
    @Transactional(readOnly = true)
    public Set<Bcid> getLatestDatasetsForExpeditions(List<Expedition> expeditions) {
        Assert.notEmpty(expeditions);

        List<Integer> expeditionIds = new ArrayList<>();

        for (Expedition expedition: expeditions)
            expeditionIds.add(expedition.getExpeditionId());

        return bcidRepository.findLatestDatasetsForExpeditions(expeditionIds);
    }

    public void delete(int bcidId) {
        bcidRepository.deleteByBcidId(bcidId);
    }

    /**
     * Entity Bcids are all Bcids with resourceType != Dataset or Expedition resourceType
     * @param expeditionId
     * @return
     */
    @Transactional(readOnly = true)
    public Set<Bcid> getEntityBcids(int expeditionId) {
        return bcidRepository.findByExpeditionExpeditionIdAndResourceTypeNotIn(expeditionId,
                ResourceTypes.DATASET_RESOURCE_TYPE, Expedition.EXPEDITION_RESOURCE_TYPE);
    }

    @Transactional(readOnly = true)
    public Set<Bcid> getBcidsWithEzidRequest() {
        return bcidRepository.findAllByEzidRequestTrue();
    }

    private void createEzid(Bcid bcid) {
        // Create EZIDs right away for Bcid level Identifiers
        // Initialize ezid account
        // NOTE: On any type of EZID error, we DON'T want to fail the process.. This means we need
        // a separate mechanism on the server side to check creation of EZIDs.  This is easy enough to do
        // in the Database.

        ManageEZID creator = new ManageEZID();
        try {
            EzidService ezidAccount = new EzidService();
            // Setup EZID account/login information
            ezidAccount.login(settingsManager.retrieveValue("eziduser"), settingsManager.retrieveValue("ezidpass"));
            creator.createBcidsEZIDs(ezidAccount);
        } catch (EzidException e) {
            logger.warn("EZID NOT CREATED FOR BCID = " + bcid.getIdentifier(), e);
        }
    }

    private URI generateBcidIdentifier(int bcidId, int naan) throws URISyntaxException {
        String bow = scheme + "/" + naan + "/";

        // Create the shoulder Bcid (String Bcid Bcid)
        String shoulder = bcidEncoder.encode(new BigInteger(String.valueOf(bcidId)));

        // Create the identifier
        return new URI(bow + shoulder);
    }

    public static void main(String[] args)  throws Exception{
        ApplicationContext applicationContext = new ClassPathXmlApplicationContext("/applicationContext.xml");
        BcidService bcidService = applicationContext.getBean(BcidService.class);
        UserRepository userService = applicationContext.getBean(UserRepository.class);
        ExpeditionRepository expeditionRepository = applicationContext.getBean(ExpeditionRepository.class);

//        User user = userService.findByUserId(8);
//        user.setEmail("test@email.com");
//        Expedition expedition = expeditionRepository.findByExpeditionId(100);
//        Bcid bcid = new Bcid.BcidBuilder(user, "Resource")
//                .expedition(expedition)
//                .build();
//        bcidService.create(bcid);
        System.out.println(bcidService.getBcid(484, "Resource"));
//        System.out.println(bcidService.getBcid("ark:/21547/r2"));
    }
}
