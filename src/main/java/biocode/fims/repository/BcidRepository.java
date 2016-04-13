package biocode.fims.repository;

import biocode.fims.bcid.BcidDatabase;
import biocode.fims.bcid.ManageEZID;
import biocode.fims.dao.BcidDao;
import biocode.fims.entities.Bcid;
import biocode.fims.ezid.EzidException;
import biocode.fims.ezid.EzidService;
import biocode.fims.settings.SettingsManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

/**
 * Repository class for {@link Bcid} domain objects
 */
public class BcidRepository {
    final static Logger logger = LoggerFactory.getLogger(BcidRepository.class);

    @Autowired
    private BcidDao bcidDao;

    private static SettingsManager settingsManager = SettingsManager.getInstance();

    @Transactional
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
