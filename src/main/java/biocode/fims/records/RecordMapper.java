package biocode.fims.records;

import biocode.fims.bcid.BcidBuilder;
import biocode.fims.config.Config;
import biocode.fims.config.models.Attribute;
import biocode.fims.config.models.Entity;
import biocode.fims.fimsExceptions.FimsRuntimeException;
import biocode.fims.fimsExceptions.errorCodes.QueryCode;
import biocode.fims.query.QueryResult;
import biocode.fims.query.QueryResults;

import java.util.*;
import java.util.function.Function;

/**
 * Utility class for transforming a {@link Record} before being returned to the user.
 */
public class RecordMapper {
    private final Map<String, String> attributes;
    private final boolean includeEmpty;
    private final List<String> source;
    private final BcidBuilder bcidBuilder;

    /**
     * @param attributes   List of {@link Attribute} used to map uri -> column for each record
     * @param includeEmpty if true, the result will include entries for all {@link Attribute}s in the {@link Entity}
     */
    public RecordMapper(BcidBuilder bcidBuilder, List<Attribute> attributes, boolean includeEmpty) {
        this(bcidBuilder, attributes, includeEmpty, Collections.emptyList());
    }

    /**
     * @param bcidBuilder  {@link BcidBuilder} used to construct the bcid for a {@link Record}
     * @param attributes   List of {@link Attribute} used to map uri -> column for each record
     * @param includeEmpty if true, the result will include entries for all {@link Attribute}s in the {@link Entity}
     * @param source       specifies the record columns to return. If empty list, no filtering will occur
     */
    public RecordMapper(BcidBuilder bcidBuilder, List<Attribute> attributes, boolean includeEmpty, List<String> source) {
        this.bcidBuilder = bcidBuilder;
        this.attributes = new HashMap<>();
        this.includeEmpty = includeEmpty;
        this.source = source;

        attributes.forEach(a -> this.attributes.put(a.getUri(), a.getColumn()));
    }

    /**
     * Returns a {@link Map} of {@link Record#properties()} as column->value pairs
     */
    public Map<String, String> map(Record record) {
        boolean skipSourceFilter = source.size() == 0;

        Map<String, String> properties = new LinkedHashMap<>();

        for (Map.Entry<String, String> e : record.properties().entrySet()) {
            String col = attributes.get(e.getKey());
            if (col == null) col = e.getKey();

            if (skipSourceFilter || source.contains(col)) {
                properties.put(
                        col,
                        e.getValue()
                );
            }
        }

        if (includeEmpty) {
            for (Map.Entry<String, String> e : attributes.entrySet()) {
                String column = e.getValue();
                if (!properties.containsKey(column) && (skipSourceFilter || source.contains(column))) {
                    properties.put(
                            column,
                            record.get(e.getKey())
                    );
                }
            }
        }

        if ((skipSourceFilter || source.contains(Record.EXPEDITION_CODE)) && record.expeditionCode() != null) {
            properties.put(Record.EXPEDITION_CODE, record.expeditionCode());
        }
        if ((skipSourceFilter || source.contains(Record.PROJECT_ID)) && record.projectId() != 0) {
            properties.put("projectId", record.projectId() == 0 ? null : String.valueOf(record.projectId()));
        }

        if (skipSourceFilter || source.contains("bcid")) {
            properties.put("bcid", bcidBuilder.build(record));
        }

        return properties;
    }

    public Record mapAsRecord(Record record) {
        return new GenericRecord(map(record), record.rootIdentifier(), record.projectId(), record.expeditionCode(), false);
    }
}
