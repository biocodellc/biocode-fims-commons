package biocode.fims.query;

import biocode.fims.projectConfig.models.Attribute;
import biocode.fims.projectConfig.models.Entity;
import biocode.fims.models.records.Record;

import java.util.*;

import static biocode.fims.bcid.Identifier.ROOT_IDENTIFIER;

/**
 * @author rjewing
 */
public class QueryResult {

    private final List<Record> records;
    private final Entity entity;
    private Entity parentEntity;

    public QueryResult(List<Record> records, Entity entity) {
        this.records = records;
        this.entity = entity;
    }

    public QueryResult(List<Record> records, Entity entity, Entity parentEntity) {
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
    public List<Map<String, String>> get(boolean includeEmpty) {
        return get(includeEmpty, Collections.emptyList());
    }

    /**
     * Returns a list of {@link Record#properties()} as a {@link Map} of column->value pairs
     *
     * @param includeEmpty if true, the result will include entries for all {@link Attribute}s in the {@link Entity}
     * @param source       specifies the record columns to return. If empty list, no filtering will occur
     * @return
     */
    public List<Map<String, String>> get(boolean includeEmpty, List<String> source) {
        List<Map<String, String>> transformedRecords = new ArrayList<>();
        boolean skipSourceFilter = source.size() == 0;

        for (Record record : records) {
            Map<String, String> properties = new LinkedHashMap<>();

            for (Map.Entry<String, String> e : record.properties().entrySet()) {
                String col = entity.getAttributeColumn(e.getKey());
                if (col == null) col = e.getKey();

                // don't add rootIdentifier to results. This is only for
                // generating bcids later
                if (Objects.equals(col, ROOT_IDENTIFIER)) continue;

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

            if (skipSourceFilter || source.contains("bcid")) {
                String bcid = record.get(ROOT_IDENTIFIER);
                if (entity.isChildEntity() && entity.getUniqueKey() != null) {
                    bcid += entity.buildChildIdentifier(record.get(parentEntity.getUniqueKeyURI()), record.get(entity.getUniqueKeyURI()));
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
