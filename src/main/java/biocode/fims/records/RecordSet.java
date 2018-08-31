package biocode.fims.records;

import biocode.fims.config.models.Entity;
import biocode.fims.fimsExceptions.FimsRuntimeException;
import biocode.fims.fimsExceptions.errorCodes.DataReaderCode;
import org.apache.commons.collections.keyvalue.MultiKey;
import org.springframework.util.Assert;

import java.util.*;
import java.util.stream.Collectors;

/**
 * A group of records for a single entity in a single expedition
 *
 * @author rjewing
 */
public class RecordSet {

    private List<Record> records;
    private RecordSet parent;
    private Entity entity;
    private boolean reload;
    private String expeditionCode;
    private final Map<MultiKey, List<Record>> recordCache;
    private boolean cacheBuilt;

    private boolean deduplicated = false;

    public RecordSet(Entity entity, boolean reload) {
        Assert.notNull(entity);
        this.entity = entity;
        this.reload = reload;
        this.records = new ArrayList<>();
        this.recordCache = new HashMap<>();
    }

    public RecordSet(Entity entity, List<Record> records, boolean reload) {
        this(entity, reload);
        this.records.addAll(records);
    }

    public void setParent(RecordSet parent) {
        this.parent = parent;
    }

    public RecordSet parent() {
        return parent;
    }

    public boolean isEmpty() {
        return records.isEmpty();
    }

    public boolean reload() {
        return this.reload && entity.canReload();
    }

    public void add(Record record) {
        deduplicated = false;
        this.records.add(record);
        addToCache(record);
    }

    public List<Record> records() {
        return Collections.unmodifiableList(records);
    }

    public List<Record> recordsToPersist() {
        return Collections.unmodifiableList(
                records.stream()
                        .filter(Record::persist)
                        .collect(Collectors.toList())
        );
    }

    public void setExpeditionCode(String expeditionCode) {
        if (!Objects.equals(expeditionCode, this.expeditionCode)) {
            cacheBuilt = false; // need to rebuild cache if expeditionCode has changed
            records.stream().filter(Record::persist).forEach(r -> r.setExpeditionCode(expeditionCode));
            this.expeditionCode = expeditionCode;
        }
    }

    public String expeditionCode() {
        if (expeditionCode != null) return expeditionCode;

        expeditionCode = records.stream()
                .filter(Record::persist)
                .findFirst()
                .orElse(new GenericRecord())
                .expeditionCode();

        return expeditionCode;
    }

    public int projectId() {
        return records.stream()
                .filter(Record::persist)
                .findFirst()
                .orElse(new GenericRecord())
                .projectId();
    }

    public String conceptAlias() {
        return entity.getConceptAlias();
    }

    public Entity entity() {
        return entity;
    }

    /**
     * remove all duplicate Records. That is, if a multiple Records have the same identifier and property values,
     * remove all but 1.
     * <p>
     *
     * @throws FimsRuntimeException with {@link DataReaderCode#INVALID_RECORDS} If 2 or more Records have the same identifier, but different properties
     */
    public void removeDuplicates() {

        if (deduplicated) {
            return;
        }

        buildCache();

        List<String> invalidRecordIdentifiers = new ArrayList<>();
        List<Record> recordsToRemove = new ArrayList<>();

        String identifierUri = entity.getUniqueKeyURI();

        recordCache.values().forEach(records -> {
            if (records.size() > 1) {
                Record record = records.get(0);

                boolean isFirst = true;
                for (Record r : records) {
                    if (!record.equals(r)) {
                        invalidRecordIdentifiers.add(record.get(identifierUri));
                        break;
                    }
                    if (!isFirst) recordsToRemove.add(r);
                    isFirst = false;
                }
            }
        });

        if (invalidRecordIdentifiers.size() > 0) {
            throw new FimsRuntimeException(DataReaderCode.INVALID_RECORDS, 400, String.join(", ", invalidRecordIdentifiers));
        }

        // remove any duplicate records
        for (Record r : recordsToRemove) {
            recordCache.get(getCacheKey(r)).remove(r);
            records.remove(r);
        }

        deduplicated = true;
    }

    public void merge(List<? extends Record> records, String parentUniqueKey) {
        buildCache();
        for (Record r : records) {
            if (shouldAddRecord(r, parentUniqueKey)) {
                add(r);
            }
        }
    }

    private boolean shouldAddRecord(Record record, String parentUniqueKey) {
        List<Record> records = recordCache.get(getCacheKey(record));
        if (records == null || records.size() == 0) return true;

        // records are cached by projectId, expeditionCode, and uniqueKey
        // here we only need to check if parentUniqueKey matches
        return records.stream()
                .noneMatch(r -> (parentUniqueKey == null || record.get(parentUniqueKey).equals(r.get(parentUniqueKey))));
    }

    public boolean hasParent() {
        return parent != null;
    }

    private void buildCache() {
        if (cacheBuilt) return;
        recordCache.clear();
        records.stream().forEach(this::addToCache);
        cacheBuilt = true;
    }

    private void addToCache(Record r) {
        recordCache.computeIfAbsent(getCacheKey(r), key -> new ArrayList<>()).add(r);
    }

    private MultiKey getCacheKey(Record r) {
        String uniqueKey = entity.getUniqueKeyURI();
        return new MultiKey(r.projectId(), r.expeditionCode(), r.get(uniqueKey));
    }
}
