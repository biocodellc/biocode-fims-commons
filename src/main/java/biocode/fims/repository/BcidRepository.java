package biocode.fims.repository;

import biocode.fims.dao.BcidDao;
import biocode.fims.entities.Bcid;
import biocode.fims.settings.SettingsManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

/**
 * Repository class for {@link Bcid} domain objects
 */
public class BcidRepository {

    @Autowired
    private BcidDao bcidDao;

    private static SettingsManager settingsManager = SettingsManager.getInstance();

    @Transactional
    public void save(Bcid bcid) {
        if (bcid.isNew()) {
            int naan = new Integer(settingsManager.retrieveValue("naan"));
            bcidDao.create(bcid, naan);
        } else {
            bcidDao.update(bcid);
        }

    }

}
