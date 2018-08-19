package biocode.fims.utils;

import biocode.fims.records.Record;

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
            sb.append(k.trim()).append(v.trim());
        });

        return Hasher.hash(sb.toString());
    }
}
