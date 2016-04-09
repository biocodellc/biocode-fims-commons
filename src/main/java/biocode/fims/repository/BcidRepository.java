package biocode.fims.repository;

import biocode.fims.bcid.BcidEncoder;
import biocode.fims.dao.BcidDao;
import biocode.fims.entities.Bcid;
import biocode.fims.settings.SettingsManager;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigInteger;
import java.net.URI;
import java.net.URISyntaxException;

/**
 * Created by rjewing on 4/8/16.
 */
public class BcidRepository {

    @Autowired
    private BcidDao bcidDao;
    private BcidEncoder bcidEncoder = new BcidEncoder();

    private SettingsManager settingsManager;

    protected String scheme = "ark:";
    protected String shoulder = "fk4";

    public void save(Bcid bcid) {
        if (bcid.isNew()) {
            int bcidId = bcidDao.create(bcid);
            bcid.setBcidId(bcidId);
            bcid.setIdentifier(generateBcidIdentifier(bcidId));
        } else {
            bcidDao.update(bcid);
        }

    }

    private URI generateBcidIdentifier(int bcidId) throws URISyntaxException {
        int naan = new Integer(settingsManager.retrieveValue("naan"));
        String bow = scheme + "/" + naan+ "/";
        // Create the shoulder Bcid (String Bcid Bcid)
        shoulder = bcidEncoder.encode(new BigInteger(String.valueOf(bcidId)));

        // Create the identifier
        return new URI(bow + shoulder);
    }

}
