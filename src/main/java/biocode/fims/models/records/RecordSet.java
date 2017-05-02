package biocode.fims.models.records;


import java.util.ArrayList;
import java.util.List;

/**
 * @author rjewing
 */
public class RecordSet {

    private List<Record> records;
    private String conceptAlias; //TODO should this be the Entity itself?

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
}
