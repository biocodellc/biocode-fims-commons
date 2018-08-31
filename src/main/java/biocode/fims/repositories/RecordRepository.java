package biocode.fims.repositories;

import biocode.fims.models.Project;
import biocode.fims.rest.responses.PaginatedResponse;
import biocode.fims.records.RecordSources;
import biocode.fims.config.models.Entity;
import biocode.fims.records.Record;
import biocode.fims.records.RecordResult;
import biocode.fims.query.QueryResults;
import biocode.fims.query.dsl.Query;
import biocode.fims.run.Dataset;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;

import java.util.List;
import java.util.Map;

/**
 * @author rjewing
 */
public interface RecordRepository {

    RecordResult get(String rootIdentifier, String localIdentifier);

    List<? extends Record> getRecords(Project project, String expeditionCode, String conceptAlias, Class<? extends Record> recordType);

    List<? extends Record> getRecords(Project project, String conceptAlias, Class<? extends Record> recordType);

    void saveChildRecord(Record record, int networkId, Entity parentEntity, Entity entity);

    void saveRecord(Record record, int networkId, Entity entity);

    void saveDataset(Dataset dataset, int networkId);

    /**
     * execute the provided sql and return a list of responseTypes.
     *
     * @param <T>
     * @param sql
     * @param responseType
     * @return
     */
    <T> List<T> query(String sql, SqlParameterSource params, Class<T> responseType);

    <T> List<T> query(String sql, SqlParameterSource params, RowMapper<T> rowMapper);

    QueryResults query(Query query);

    PaginatedResponse<Map<String, List<Map<String, String>>>> query(Query query, RecordSources sources, boolean includeEmptyProperties);
}
