package biocode.fims.service;

import biocode.fims.bcid.BcidEncoder;
import biocode.fims.bcid.ManageEZID;
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
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    private final BcidRepository bcidRepository;
    private final SettingsManager settingsManager;
    private final BcidEncoder bcidEncoder = new BcidEncoder();

    @Autowired
    public BcidService(BcidRepository bcidRepository, SettingsManager settingsManager) {
        this.bcidRepository = bcidRepository;
        this.settingsManager = settingsManager;
    }

    @Transactional
    public Bcid create(Bcid bcid) {
        int naan = new Integer(settingsManager.retrieveValue("naan"));
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

        if (bcid.isEzidRequest())
            createEzid(bcid);

        return bcid;
    }

    public void update(Bcid bcid) {
        bcidRepository.save(bcid);
    }

    public Bcid getBcid(String identifier) {
        return bcidRepository.findByIdentifier(identifier);
    }

    public Bcid getBcid(int bcidId) {
        return bcidRepository.findByBcidId(bcidId);
    }

    /**
     * @param expeditionId the {@link biocode.fims.entities.Expedition} the bcids are associated with
     * @param resourceType the resourceType(s) of the Bcids to find
     * @return the {@link Bcid} associated with the provided {@link biocode.fims.entities.Expedition}, containing
     * the provided resourceType(s)
     */
    public Bcid getBcid(int expeditionId, String... resourceType) {
        return bcidRepository.findByExpeditionExpeditionIdAndResourceTypeIn(expeditionId, resourceType);
    }

    public Set<Bcid> getLatestDatasets(int projectId) {
        return bcidRepository.findLatestDatasets(projectId);
    }

    /**
     * fetch the latest Bcids with resourceType = 'http://purl.org/dc/dcmitype/Dataset' for the provided list of
     * {@link Expedition}s
     * @param expeditions
     * @return
     */
    public Set<Bcid> getLatestDatasetsForExpeditions(List<Expedition> expeditions) {
        List<Integer> expeditionIds = new ArrayList<>();

        for (Expedition expedition: expeditions)
            expeditionIds.add(expedition.getExpeditionId());

        return bcidRepository.findLatestDatasetsForExpeditions(expeditionIds);
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

        User user = userService.findByUserId(8);
        user.setEmail("test@email.com");
        Expedition expedition = expeditionRepository.findByExpeditionId(100);
        Bcid bcid = new Bcid.BcidBuilder(user, "Resource")
//                .expedition(expedition)
                .build();
        bcidService.create(bcid);
//        System.out.println(bcidService.getBcid("ark:/21547/r2"));
    }
}
