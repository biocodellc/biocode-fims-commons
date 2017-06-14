package biocode.fims.repositories;

import biocode.fims.models.records.Record;
import biocode.fims.query.QueryResult;
import biocode.fims.query.dsl.Query;
import biocode.fims.run.Dataset;
import org.springframework.data.domain.Page;

import java.util.List;
import java.util.Map;

/**
 * @author rjewing
 */
public interface RecordRepository {

    List<? extends Record> getRecords(int projectId, String expeditionCode, String conceptAlias, Class<? extends Record> recordType);

    void save(Dataset dataset, int projectId, int expeditionId);

    QueryResult query(Query query);

    Page<Map<String, String>> query(Query query, int page, int limit, boolean includeEmptyProperties);
}
