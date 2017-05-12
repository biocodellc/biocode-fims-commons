package biocode.fims.repositories;

import biocode.fims.models.records.Record;
import biocode.fims.models.records.RecordSet;

import java.util.List;

/**
 * @author rjewing
 */
public interface RecordRepository {

    List<Record> getRecords(int projectId, String expeditionCode, String conceptAlias);

    void save(List<RecordSet> recordSets, String projectCode, int expeditionId);
}
