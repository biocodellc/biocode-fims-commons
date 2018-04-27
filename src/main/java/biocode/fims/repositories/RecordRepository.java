package biocode.fims.repositories;

import biocode.fims.models.records.Record;
import biocode.fims.models.records.RecordResult;
import biocode.fims.query.QueryResult;
import biocode.fims.query.QueryResults;
import biocode.fims.query.dsl.Query;
import biocode.fims.run.Dataset;
import org.springframework.data.domain.Page;

import java.util.List;
import java.util.Map;

/**
 * @author rjewing
 */
public interface RecordRepository {

    RecordResult get(String rootIdentifier, String localIdentifier);

    List<? extends Record> getRecords(int projectId, String expeditionCode, String conceptAlias, Class<? extends Record> recordType);

    void save(Dataset dataset, int projectId, int expeditionId);

    QueryResults query(Query query);

    Page<Map<String, String>> query(Query query, int page, int limit, List<String> source, boolean includeEmptyProperties);
}
