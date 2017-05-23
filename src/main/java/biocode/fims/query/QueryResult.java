package biocode.fims.query;

import biocode.fims.digester.Attribute;
import biocode.fims.digester.Entity;
import biocode.fims.models.records.Record;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author rjewing
 */
public class QueryResult {

    private final List<Record> records;
    private final Entity entity;

    public QueryResult(List<Record> records, Entity entity) {
        this.records = records;
        this.entity = entity;
    }

    public Entity entity() {
        return entity;
    }

    /**
     * Returns a list of {@link Record#properties()} as a {@link Map} of column->value pairs
     *
     * @param includeEmpty if true, the result will include entries for all {@link Attribute}s in the {@link Entity}
     * @return
     */
    public List<Map<String, String>> get(boolean includeEmpty) {
        List<Map<String, String>> transformedRecords = new ArrayList<>();

        for (Record record : records) {
            Map<String, String> properties = new HashMap<>();

            if (!includeEmpty) {
                for (Map.Entry<String, String> e : record.properties().entrySet()) {
                    properties.put(
                            entity.getAttributeColumn(e.getKey()),
                            e.getValue()
                    );
                }
            } else {
                for (Attribute a : entity.getAttributes()) {
                    properties.put(
                            a.getColumn(),
                            record.get(a.getUri())
                    );
                }
            }

            transformedRecords.add(properties);
        }

        return transformedRecords;
    }
}
