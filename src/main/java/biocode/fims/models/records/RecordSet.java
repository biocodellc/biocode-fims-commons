package biocode.fims.models.records;


import biocode.fims.digester.Entity;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

/**
 * @author rjewing
 */
public class RecordSet {

    private List<Record> records;
    private String conceptAlias; //TODO should this be the Entity itself?
    private Entity entity;

    public RecordSet(Entity entity) {
        this.entity = entity;
        this.conceptAlias = entity.getConceptAlias();
        this.records = new ArrayList<>();
    }

    public RecordSet(Entity entity, List<Record> records) {
        this(entity);
        this.records.addAll(records);
    }

    public RecordSet(String conceptAlias) {
        this.conceptAlias = conceptAlias;
        this.records = new ArrayList<>();
    }

    public RecordSet(String conceptAlias, List<Record> records) {
        this(conceptAlias);
        this.records.addAll(records);
    }

    public boolean isEmpty() {
        return records.isEmpty();
    }

    public void add(Record record) {
        this.records.add(record);
    }

    public List<Record> records() {
        return records;
    }

    public String conceptAlias() {
        return conceptAlias;
    }

    public void merge(List<Record> records) {

        for (Record r : records) {
            if (addRecord(r)) {
                this.records.add(r);
            }
        }

    }

    private boolean addRecord(Record record) {
        String uniqueKey = entity.getUniqueKey();

        return records.stream()
                .noneMatch(r -> record.get(uniqueKey).equals(r.get(uniqueKey)));
    }
}
