package biocode.fims.repository;

import biocode.fims.bcid.ManageEZID;
import biocode.fims.dao.BcidDao;
import biocode.fims.entities.Bcid;
import biocode.fims.ezid.EzidException;
import biocode.fims.ezid.EzidService;
import biocode.fims.settings.SettingsManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * Repository class for {@link Bcid} domain objects
 */
@Repository
public class BcidRepository {
    final static Logger logger = LoggerFactory.getLogger(BcidRepository.class);

    private BcidDao bcidDao;
    private SettingsManager settingsManager;

    @Autowired
    public BcidRepository(BcidDao bcidDao, SettingsManager settingsManager) {
        this.bcidDao = bcidDao;
        this.settingsManager = settingsManager;
    }

    //TODO get @Transactional working
//    @Transactional
    public void save(Bcid bcid) {
        if (bcid.isNew()) {
            int naan = new Integer(settingsManager.retrieveValue("naan"));
            bcidDao.create(bcid, naan);
            if (bcid.isEzidRequest())
                createEzid(bcid);
        } else {
            bcidDao.update(bcid);
        }

    }

    /**
     * @param identifier the identifier of the {@link Bcid} to fetch
     * @return the {@link Bcid} with the provided identifier
     */
    public Bcid findByIdentifier(String identifier) {
        Map<String, String> params = new HashMap<>();
        params.put("identifier", identifier);
        return bcidDao.findBcid(params);
    }

    /**
     * @param expeditionId the {@link biocode.fims.entities.Expedition} the bcids are associated with
     * @param resourceType the resourceType of the Bcids to find
     * @return the {@link Bcid} Collection associated with the provided {@link biocode.fims.entities.Expedition}, containing
     * the provided resourceType
     */
    public Collection<Bcid> findByExpeditionAndResourceType(int expeditionId, String resourceType) {
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("expeditionId", expeditionId)
                .addValue("resourceType", resourceType);

        return bcidDao.findBcidsAssociatedWithExpedition(params);
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

}
