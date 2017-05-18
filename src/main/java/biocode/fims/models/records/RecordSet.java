package biocode.fims.models.records;


import biocode.fims.digester.Entity;
import biocode.fims.fimsExceptions.FimsRuntimeException;
import biocode.fims.fimsExceptions.errorCodes.DataReaderCode;
import org.springframework.util.Assert;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @author rjewing
 */
public class RecordSet {

    private List<Record> records;
    private RecordSet parent;
    private Entity entity;

    private boolean deduplicated = false;

    public RecordSet(Entity entity) {
        Assert.notNull(entity);
        this.entity = entity;
        this.records = new ArrayList<>();
    }

    public RecordSet(Entity entity, List<Record> records) {
        this(entity);
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

    public void add(Record record) {
        deduplicated = false;
        this.records.add(record);
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

        List<String> invalidRecordIdentifiers = new ArrayList<>();
        Map<String, Record> recordMap = new HashMap<>();

        String identifierUri = entity.getUniqueKeyURI();

        for (Record r : records()) {
            String identifier = r.get(identifierUri);

            if (recordMap.containsKey(identifier)) {

                if (!recordMap.get(identifier).equals(r)) {
                    invalidRecordIdentifiers.add(identifier);
                }

            } else {
                recordMap.put(identifier, r);
            }
        }

        if (invalidRecordIdentifiers.size() > 0) {
            throw new FimsRuntimeException(DataReaderCode.INVALID_RECORDS, 400, String.join(", ", invalidRecordIdentifiers));
        }

        // remove any duplicate records
        records = new LinkedList<>(new LinkedHashSet<>(records));

        deduplicated = true;
    }

    public void merge(List<? extends Record> records) {
        for (Record r : records) {
            if (addRecord(r)) {
                this.records.add(r);
            }
        }
    }

    private boolean addRecord(Record record) {
        String uniqueKey = entity.getUniqueKeyURI();

        return records.stream()
                .noneMatch(r -> record.get(uniqueKey).equals(r.get(uniqueKey)));
    }

    public boolean hasParent() {
        return parent != null;
    }
}
