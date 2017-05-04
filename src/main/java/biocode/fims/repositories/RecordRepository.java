package biocode.fims.repositories;

import biocode.fims.models.records.Record;

import java.util.List;

/**
 * @author rjewing
 */
public interface RecordRepository {

    List<Record> getRecords(int projectId, String expeditionCode, String conceptAlias);
}
