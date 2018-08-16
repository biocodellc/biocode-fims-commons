package biocode.fims.utils;

import biocode.fims.models.records.Record;
import org.apache.commons.lang.StringUtils;

import java.util.TreeMap;

/**
 * @author rjewing
 */
public class RecordHasher {
    /**
     * Generate a hash of the non-empty record properties
     *
     * @param r
     * @return
     */
    public static String hash(Record r) {
        // will sort by natural ordering of keys
        TreeMap<String, String> sortedMap = new TreeMap<>(r.properties());

        StringBuilder sb = new StringBuilder();

        sortedMap.forEach((k, v) -> {
            if (!StringUtils.isBlank(v)) {
                sb.append(v.trim());
            }
        });

        return Hasher.hash(sb.toString());
    }
}
