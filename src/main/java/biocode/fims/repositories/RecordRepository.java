package biocode.fims.repositories;

import biocode.fims.digester.Entity;
import biocode.fims.models.records.Record;
import biocode.fims.models.records.RecordResult;
import biocode.fims.query.QueryResult;
import biocode.fims.query.QueryResults;
import biocode.fims.query.dsl.Query;
import biocode.fims.run.Dataset;
import org.springframework.data.domain.Page;
import org.springframework.jdbc.core.RowMapper;

import java.util.List;
import java.util.Map;

/**
 * @author rjewing
 */
public interface RecordRepository {

    RecordResult get(String rootIdentifier, String localIdentifier);

    List<? extends Record> getRecords(int projectId, String expeditionCode, String conceptAlias, Class<? extends Record> recordType);

    void saveChildRecord(Record record, int projectId, Entity parentEntity, Entity entity, int expeditionId);

    void saveRecord(Record record, int projectId, Entity entity, int expeditionId);

    void saveDataset(Dataset dataset, int projectId, int expeditionId);

    /**
     * execute the provided sql and return a list of responseTypes.
     *
     * @param <T>
     * @param sql
     * @param responseType
     * @return
     */
    <T> List<T> query(String sql, Map<String, String> paramMap, Class<T> responseType);

    <T> List<T> query(String sql, Map<String, String> paramMap, RowMapper<T> rowMapper);

    QueryResults query(Query query);

    Page<Map<String, String>> query(Query query, int page, int limit, List<String> source, boolean includeEmptyProperties);
}
