package biocode.fims.models.records;

/**
 * Class to handle result from Record lookup by arkID
 *
 * @author rjewing
 */
public class RecordResult {
    private int expeditionId;
    private int projectId;
    private Record record;
    private String conceptAlias;

    public RecordResult(int projectId, int expeditionId, String conceptAlias, Record record) {
        this.projectId = projectId;
        this.expeditionId = expeditionId;
        this.conceptAlias = conceptAlias;
        this.record = record;
    }

    public int projectId() {
        return projectId;
    }

    public int expeditionId() {
        return expeditionId;
    }

    public Record record() {
        return record;
    }

    public String conceptAlias() {
        return conceptAlias;
    }
}

