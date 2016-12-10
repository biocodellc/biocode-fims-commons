package biocode.fims.rest.versioning;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * domain object for holding information about versioning resource locations
 *
 * @author RJ Ewing
 */
public class VersionUrlConfig {

    private Map<String, List<VersionUrlData>> versionMap = new HashMap<>();

    public Map<String, List<VersionUrlData>> getVersionMap() {
        return versionMap;
    }

    public void setVersionMap(Map<String, List<VersionUrlData>> versionMap) {
        this.versionMap = versionMap;
    }

    public static class VersionUrlData {

        private String versionUrl;
        private String currentUrl;
        private List<String> namedGroups = new ArrayList<>();

        public String getVersionUrl() {
            return versionUrl;
        }

        public void setVersionUrl(String versionUrl) {
            this.versionUrl = versionUrl;
        }

        public String getCurrentUrl() {
            return currentUrl;
        }

        public void setCurrentUrl(String currentUrl) {
            this.currentUrl = currentUrl;
        }

        public List<String> getNamedGroups() {
            return namedGroups;
        }

        public void setNamedGroups(List<String> namedGroups) {
            this.namedGroups = namedGroups;
        }
    }
}
