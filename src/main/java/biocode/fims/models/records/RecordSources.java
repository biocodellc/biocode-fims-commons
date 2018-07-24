package biocode.fims.models.records;

import java.util.*;

/**
 * @author rjewing
 */
public class RecordSources {
    Map<String, List<String>> sources;

    public RecordSources(Map<String, List<String>> sources) {
        this.sources = sources;
    }

    public List<String> get(String entity) {
        return sources.getOrDefault(entity, new ArrayList<>());
    }

    /**
     * @param rawSources array of source strings. ex. (Sample.materialSampleID, Event.eventID, etc) If there is no
     *                   entity prefix, then the column is assumed to be an atribute of the defaultEntity
     * @param defaultEntity conceptAlias of the entity to default to
     * @return
     */
    public static RecordSources factory(List<String> rawSources, String defaultEntity) {
        Map<String, List<String>> sources = new HashMap<>();

        for (String source: rawSources) {
            List<String> split = new ArrayList<>(Arrays.asList(source.split("\\.")));
            if (split.size() == 1) {
                sources.computeIfAbsent(defaultEntity, k -> new ArrayList<>()).add(split.get(0));
            } else {
                String entity = split.remove(0);
                sources.computeIfAbsent(entity, k -> new ArrayList<>()).add(String.join(".", split));
            }
        }

        return new RecordSources(sources);
    }
}
