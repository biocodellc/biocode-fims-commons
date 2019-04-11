package biocode.fims.query;

import biocode.fims.bcid.BcidBuilder;
import biocode.fims.config.models.Attribute;
import biocode.fims.config.models.Entity;
import biocode.fims.records.GenericRecord;
import biocode.fims.records.Record;
import biocode.fims.records.RecordMapper;

import java.util.*;
import java.util.stream.Collectors;

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
    public LinkedList<Map<String, Object>> get(boolean includeEmpty) {
        return get(includeEmpty, Collections.emptyList());
    }

    /**
     * Returns a list of {@link Record#properties()} as a {@link Map} of column->value pairs
     *
     * @param includeEmpty if true, the result will include entries for all {@link Attribute}s in the {@link Entity}
     * @param source       specifies the record columns to return. If empty list, no filtering will occur
     * @return
     */
    public LinkedList<Map<String, Object>> get(boolean includeEmpty, List<String> source) {
        RecordMapper recordMapper = getRecordMapper(includeEmpty, source);
        return records.stream()
                .map(recordMapper::map)
                .collect(Collectors.toCollection(LinkedList::new));
    }

    public LinkedList<Record> getAsRecord(boolean includeEmpty) {
        return getAsRecord(includeEmpty, Collections.emptyList());
    }

    public LinkedList<Record> getAsRecord(boolean includeEmpty, List<String> source) {
        RecordMapper recordMapper = getRecordMapper(includeEmpty, source);
        return records.stream()
                .map(recordMapper::mapAsRecord)
                .collect(Collectors.toCollection(LinkedList::new));
    }

    private RecordMapper getRecordMapper(boolean includeEmpty, List<String> source) {
        BcidBuilder bcidBuilder = new BcidBuilder(entity, parentEntity);
        return new RecordMapper(bcidBuilder, entity.getAttributes(), includeEmpty, source);
    }
}
