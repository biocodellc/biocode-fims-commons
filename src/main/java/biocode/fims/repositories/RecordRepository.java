package biocode.fims.repositories;

import biocode.fims.models.records.GenericRecord;
import biocode.fims.models.records.Record;
import biocode.fims.models.records.RecordSet;

import java.util.List;

/**
 * @author rjewing
 */
public interface RecordRepository {

    List<? extends Record> getRecords(int projectId, String expeditionCode, String conceptAlias, Class<? extends Record> recordType);

    void save(List<RecordSet> recordSets, String projectCode, int expeditionId);
}
