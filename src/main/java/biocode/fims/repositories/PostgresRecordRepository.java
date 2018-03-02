package biocode.fims.repositories;

import biocode.fims.digester.Entity;
import biocode.fims.fimsExceptions.FimsRuntimeException;
import biocode.fims.fimsExceptions.errorCodes.UploadCode;
import biocode.fims.models.records.GenericRecord;
import biocode.fims.models.records.GenericRecordRowMapper;
import biocode.fims.models.records.Record;
import biocode.fims.models.records.RecordSet;
import biocode.fims.query.ParametrizedQuery;
import biocode.fims.query.PostgresUtils;
import biocode.fims.query.QueryResult;
import biocode.fims.query.dsl.Query;
import biocode.fims.rest.FimsObjectMapper;
import biocode.fims.run.Dataset;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.text.StrSubstitutor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

/**
 * @author rjewing
 */
@Transactional
public class PostgresRecordRepository implements RecordRepository {
    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final Properties sql;
    private final Map<Class<? extends Record>, RowMapper<? extends Record>> rowMappers;

    public PostgresRecordRepository(NamedParameterJdbcTemplate jdbcTemplate, Properties sql, Map<Class<? extends Record>, RowMapper<? extends Record>> rowMappers) {
        this.jdbcTemplate = jdbcTemplate;
        this.sql = sql;
        this.rowMappers = rowMappers;
    }

    @Override
    public List<? extends Record> getRecords(int projectId, String expeditionCode, String conceptAlias, Class<? extends Record> recordType) {
        Map<String, Object> tableMap = getTableMap(projectId, conceptAlias);

        Map<String, Object> sqlParams = new HashMap<>();
        sqlParams.put("expeditionCode", expeditionCode);
        sqlParams.put("projectId", projectId);

        return jdbcTemplate.query(
                StrSubstitutor.replace(sql.getProperty("selectRecords"), tableMap),
                sqlParams,
                rowMappers.getOrDefault(recordType, new GenericRecordRowMapper())
        );
    }

    @Override
    @SetFimsUser
    @SuppressWarnings({"unchecked"})
    public void save(Dataset dataset, int projectId, int expeditionId) {
        try {
            for (RecordSet recordSet : dataset) {

                String table = recordSet.conceptAlias();
                String localIdentifierUri = recordSet.entity().getUniqueKeyURI();

                if (StringUtils.isBlank(table)) {
                    throw new IllegalStateException("entity conceptAlias must not be null");
                }

                Map<String, Object> tableMap = getTableMap(projectId, table);

                List<HashMap<String, ?>> insertParams = new ArrayList<>();
                ObjectMapper mapper = new FimsObjectMapper();
                List<String> localIdentifiers = new ArrayList<>();
                List<String> parentIdentifiers = new ArrayList<>();

                for (Record record : recordSet.recordsToPersist()) {

                    HashMap<String, Object> recordParams = new HashMap<>();
                    recordParams.put("expeditionId", expeditionId);
                    String localIdentifier = record.get(localIdentifierUri);
                    recordParams.put("identifier", localIdentifier);
                    localIdentifiers.add(localIdentifier);

                    if (recordSet.hasParent()) {
                        String parentEntityIdColumn = recordSet.parent().entity().getUniqueKey();
                        String parentIdentifierUri = recordSet.entity().getAttributeUri(parentEntityIdColumn);
                        String parentIdentifier = record.get(parentIdentifierUri);
                        recordParams.put("parent_identifier", parentIdentifier);
                        parentIdentifiers.add(parentIdentifier);
                    }

                    Map<String, String> properties = record.properties();
                    recordParams.put("data", mapper.writeValueAsString(removeEmptyProperties(properties)));

                    insertParams.add(recordParams);
                }

                String sqlString;
                if (recordSet.hasParent()) {
                    sqlString = sql.getProperty("insertChildRecord");
                } else {
                    sqlString = sql.getProperty("insertRecord");
                }

                jdbcTemplate.batchUpdate(
                        StrSubstitutor.replace(sqlString, tableMap),
                        insertParams.toArray(new HashMap[insertParams.size()])
                );

                // Delete any records not in the current RecordSet
                if (recordSet.reload()) {
                    String deleteSql;
                    HashMap<String, Object> deleteParams = new HashMap<>();
                    deleteParams.put("expeditionId", expeditionId);

                    if (recordSet.hasParent()) {
                        deleteSql = sql.getProperty("deleteChildRecords");
                        List<Object[]> identifierTuples = new ArrayList<>(localIdentifiers.size());

                        for (int i = 0; i < localIdentifiers.size(); i++) {
                            identifierTuples.add(
                                    new Object[]{parentIdentifiers.get(i), localIdentifiers.get(i)}
                            );
                        }

                        deleteParams.put("identifiers", identifierTuples);
                    } else {
                        deleteSql = sql.getProperty("deleteRecords");
                        deleteParams.put("identifiers", localIdentifiers);
                    }

                    jdbcTemplate.update(
                            StrSubstitutor.replace(deleteSql, tableMap),
                            deleteParams
                    );
                }

            }
        } catch (JsonProcessingException e) {
            throw new FimsRuntimeException(UploadCode.DATA_SERIALIZATION_ERROR, 500);
        }
    }

    @Override
    @SuppressWarnings({"unchecked"})
    public QueryResult query(Query query) {
        boolean onlyPublicExpeditions = query.expeditions().isEmpty();
        ParametrizedQuery q = query.parameterizedQuery(onlyPublicExpeditions);
        RecordRowCallbackHandler handler = new RecordRowCallbackHandler(query.entity());
        jdbcTemplate.query(q.sql(), q.params(), handler);

        return new QueryResult(handler.records, query.entity(), handler.rootIdentifier);
    }

    @Override
    @SuppressWarnings({"unchecked"})
    public Page<Map<String, String>> query(Query query, int page, int limit, boolean includeEmptyProperties) {
        // naive implementation that will work for now. Probably better to use postgres to paginate
        boolean onlyPublicExpeditions = query.expeditions().isEmpty();
        ParametrizedQuery q = query.parameterizedQuery(onlyPublicExpeditions);

        RecordRowCallbackHandler handler = new RecordRowCallbackHandler(query.entity());
        jdbcTemplate.query(q.sql(), q.params(), handler);

        int total = handler.records.size();
        int from = page * limit;
        int to = (from + limit < total) ? from + limit : total;

        QueryResult queryResult = new QueryResult(handler.records.subList(from, to), query.entity(), handler.rootIdentifier);

        Pageable pageable = new PageRequest(page, limit);
        return new PageImpl<>(queryResult.get(includeEmptyProperties), pageable, total);
    }


    private Map<String, Object> getTableMap(int projectId, String conceptAlias) {
        Map<String, Object> tableMap = new HashMap<>();
        tableMap.put("table", PostgresUtils.entityTable(projectId, conceptAlias));
        return tableMap;
    }

    private Map<String, String> removeEmptyProperties(Map<String, String> properties) {
        Map<String, String> nonEmptyProps = new HashMap<>();

        for (Map.Entry<String, String> entry : properties.entrySet()) {
            String value = entry.getValue();

            if (value.trim().equals("")) {
                continue;
            }

            nonEmptyProps.put(entry.getKey(), value);
        }

        return nonEmptyProps;
    }

    private class RecordRowCallbackHandler implements RowCallbackHandler {
        private RowMapper<? extends Record> rowMapper;
        final List<Record> records;
        String rootIdentifier;

        private RecordRowCallbackHandler(Entity entity) {
            this.rowMapper = rowMappers.get(entity.getRecordType());
            if (this.rowMapper == null) {
                this.rowMapper = rowMappers.get(GenericRecord.class);
            }
            this.records = new ArrayList<>();
        }


        @Override
        public void processRow(ResultSet rs) throws SQLException {
            if (rootIdentifier == null) {
                rootIdentifier = rs.getString("root_identifier");
            }
            records.add(rowMapper.mapRow(rs, records.size() - 1));
        }

    }
}
