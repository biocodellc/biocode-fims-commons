package biocode.fims.ezid;

import biocode.fims.models.Bcid;
import biocode.fims.models.User;
import biocode.fims.settings.SettingsManager;
import biocode.fims.utils.EmailUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.HashMap;
import java.util.Map;

/**
 * Class to help with EZID creation
 */
public class EzidUtils {
    private static final String DEFAULT_PUBLISHER = "Biocode FIMS System";
    private final SettingsManager settingsManager;

    @Autowired
    public EzidUtils(SettingsManager settingsManager) {
        this.settingsManager = settingsManager;
    }

    private HashMap<String, String> ercMap(String target, String what, String who, String when) {
        HashMap<String, String> map = new HashMap<String, String>();
        map.put("_profile", "erc");

        // _target needs to be resolved by biscicol for now
        map.put("_target", target);
        // what is always dataset
        map.put("erc.what", what);
        // who is the user who loaded this
        map.put("erc.who", who);
        // when is timestamp of data loading
        map.put("erc.when", when);
        return map;
    }

    private HashMap<String, String> dcMap(String target, String creator, String title, String publisher, String when, String type) {
        HashMap<String, String> map = new HashMap<String, String>();
        map.put("_profile", "dc");
        // _target needs to be resolved by biscicol for now
        map.put("_target", target);
        map.put("dc.creator", creator);
        map.put("dc.title", title);
        map.put("dc.publisher", publisher);
        map.put("dc.date", when);
        map.put("dc.type", type);
        map.put("_profile", "dc");
        return map;
    }

    public HashMap<String, String> getDcMap(Bcid bcid) {
        String publisher = this.settingsManager.retrieveValue("publisher");
        if (StringUtils.isBlank(publisher)) {
            publisher = DEFAULT_PUBLISHER;
        }

        // Get creator, using any system defined creator to override the default which is based on user data
        String creator = this.settingsManager.retrieveValue("creator");
        if (StringUtils.isBlank(creator)) {
            creator = getEzidCreatorField(bcid);
        }
        String resolverTargetPrefix = settingsManager.retrieveValue("resolverTargetPrefix");

        return dcMap(
                resolverTargetPrefix + bcid.getIdentifier(),
                creator,
                bcid.getTitle(),
                publisher,
                String.valueOf(bcid.getModified()),
                bcid.getResourceType());
    }

    private String getEzidCreatorField(Bcid bcid) {
        User bcidUser = bcid.getUser();
        return bcidUser.getFirstName() + " " + bcidUser.getLastName() + " <" + bcidUser.getEmail() + ">";
    }

    /**
     * send an email report for the failed Ezids
     * @param errorMap key = Bcid.identifier, value = stacktrace
     */
    public void sendErrorEmail(Map<String, String> errorMap) {
        StringBuilder sb = new StringBuilder();
        sb.append("EZIDs were not created for the following Bcids: \n\n\n");

        for (Map.Entry<String, String> error: errorMap.entrySet()) {
            sb.append("identifier: ");
            sb.append(error.getKey());
            sb.append("\n");

            sb.append("\t");
            sb.append(error.getValue());
            sb.append("\n\n");
        }

        // Send an Email that this completed
        EmailUtils.sendAdminEmail(
                "Error creating Ezid(s)",
                sb.toString()
        );
    }
}
