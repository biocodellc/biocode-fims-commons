package biocode.fims.query;

import biocode.fims.projectConfig.models.Attribute;
import biocode.fims.projectConfig.models.Entity;
import biocode.fims.records.Record;

import java.util.*;

/**
 * @author rjewing
 */
public class QueryResult {

    private final LinkedList<Record> records;
    private final Entity entity;
    private Entity parentEntity;

    public QueryResult(LinkedList<Record> records, Entity entity) {
        this.records = records;
        this.entity = entity;
    }

    public QueryResult(LinkedList<Record> records, Entity entity, Entity parentEntity) {
        this(records, entity);
        this.parentEntity = parentEntity;
    }

    public Entity entity() {
        return entity;
    }

    public List<Record> records() {
        return Collections.unmodifiableList(records);
    }

    /**
     * Returns a list of {@link Record#properties()} as a {@link Map} of column->value pairs
     *
     * @param includeEmpty if true, the result will include entries for all {@link Attribute}s in the {@link Entity}
     * @return
     */
    public LinkedList<Map<String, String>> get(boolean includeEmpty) {
        return get(includeEmpty, Collections.emptyList());
    }

    /**
     * Returns a list of {@link Record#properties()} as a {@link Map} of column->value pairs
     *
     * @param includeEmpty if true, the result will include entries for all {@link Attribute}s in the {@link Entity}
     * @param source       specifies the record columns to return. If empty list, no filtering will occur
     * @return
     */
    public LinkedList<Map<String, String>> get(boolean includeEmpty, List<String> source) {
        LinkedList<Map<String, String>> transformedRecords = new LinkedList<>();
        boolean skipSourceFilter = source.size() == 0;

        for (Record record : records) {
            Map<String, String> properties = new LinkedHashMap<>();

            for (Map.Entry<String, String> e : record.properties().entrySet()) {
                String col = entity.getAttributeColumn(e.getKey());
                if (col == null) col = e.getKey();

                if (skipSourceFilter || source.contains(col)) {
                    properties.put(
                            col,
                            e.getValue()
                    );
                }
            }

            if (includeEmpty) {
                for (Attribute a : entity.getAttributes()) {
                    if (!properties.containsKey(a.getColumn()) && (skipSourceFilter || source.contains(a.getColumn()))) {
                        properties.put(
                                a.getColumn(),
                                record.get(a.getUri())
                        );
                    }
                }
            }

            if ((skipSourceFilter || source.contains("expeditionCode")) && record.expeditionCode() != null) {
                properties.put("expeditionCode", record.expeditionCode());
            }
            if ((skipSourceFilter || source.contains("projectId")) && record.projectId() != 0 ) {
                properties.put("projectId", record.projectId() == 0 ? null : String.valueOf(record.projectId()));
            }

            if (skipSourceFilter || source.contains("bcid")) {
                String bcid = record.rootIdentifier();
                if (entity.isChildEntity() && entity.getUniqueKey() != null) {
                    bcid += record.get(entity.getUniqueKeyURI());
                } else if (entity.isChildEntity()) {
                    bcid += record.get(parentEntity.getUniqueKeyURI());
                } else {
                    bcid += record.get(entity.getUniqueKeyURI());
                }
                properties.put("bcid", bcid);
            }

            transformedRecords.add(properties);
        }

        return transformedRecords;
    }
}
