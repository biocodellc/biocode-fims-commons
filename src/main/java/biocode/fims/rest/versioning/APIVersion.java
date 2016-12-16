package biocode.fims.rest.versioning;

import java.util.*;

/**
 * enum containing rest api versions
 */
public enum APIVersion {
    // must be declared in ascending order
    v1_0("1_0", new String[]{"v1", "v1.0"}),
    v1_1("1_1", new String[]{"v1.1"});

    public static String DEFAULT_VERSION = "v1";

    private String transformerSuffix;
    private String[] names;

    APIVersion(String transformerSuffix, String[] names) {
        this.transformerSuffix = transformerSuffix;
        this.names = names;
    }

    public String getTransformerSuffix() {
        return transformerSuffix;
    }

    public static APIVersion version(String versionName) {
        for (APIVersion apiVersion : values()) {
            for (String name : apiVersion.names) {
                if (name.equalsIgnoreCase(versionName))
                    return apiVersion;
            }
        }
        throw new IllegalArgumentException();
    }

    /**
     * get a list of all APIVersions from the provided version to the latest version
     *
     * @param startingVersionName
     * @return
     */
    public static List<APIVersion> range(String startingVersionName) {
        List<APIVersion> orderedVersions = Arrays.asList(values());

        APIVersion startingVersion = version(startingVersionName);

        return new ArrayList<>(orderedVersions.subList(orderedVersions.indexOf(startingVersion), orderedVersions.size()));
    }

    @Override
    public String toString() {
        return name() + " names: " + Arrays.toString(names);
    }
}
