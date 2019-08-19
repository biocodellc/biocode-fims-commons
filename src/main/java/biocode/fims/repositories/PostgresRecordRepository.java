package biocode.fims.repositories;

import biocode.fims.application.config.FimsProperties;
import biocode.fims.models.Project;
import biocode.fims.records.*;
import biocode.fims.rest.responses.PaginatedResponse;
import biocode.fims.config.models.Entity;
import biocode.fims.fimsExceptions.FimsRuntimeException;
import biocode.fims.fimsExceptions.errorCodes.QueryCode;
import biocode.fims.fimsExceptions.errorCodes.UploadCode;
import biocode.fims.models.dataTypes.JacksonUtil;
import biocode.fims.query.ParametrizedQuery;
import biocode.fims.query.PostgresUtils;
import biocode.fims.query.QueryResult;
import biocode.fims.query.QueryResults;
import biocode.fims.query.dsl.Query;
import biocode.fims.rest.FimsObjectMapper;
import biocode.fims.run.Dataset;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.text.StringSubstitutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.transaction.annotation.Transactional;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.*;

/**
 * @author rjewing
 */
@Transactional
public class PostgresRecordRepository implements RecordRepository {
    private final static Logger logger = LoggerFactory.getLogger(PostgresRecordRepository.class);
    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final Properties sql;
    private final Map<Class<? extends Record>, FimsRowMapper<? extends Record>> rowMappers;
    private final FimsProperties fimsProperties;

    public PostgresRecordRepository(NamedParameterJdbcTemplate jdbcTemplate, Properties sql, Map<Class<? extends Record>, FimsRowMapper<? extends Record>> rowMappers, FimsProperties fimsProperties) {
        this.jdbcTemplate = jdbcTemplate;
        this.sql = sql;
        this.rowMappers = rowMappers;
        this.fimsProperties = fimsProperties;
    }

    @Override
    public RecordResult get(String rootIdentifier, String localIdentifier) {
        EntityIdentifierResult result = getEntityIdentifierResult(rootIdentifier);

        if (result == null) return null;

        Map<String, Object> tableMap = PostgresUtils.getTableMap(result.networkId, result.conceptAlias);
        tableMap.put("rootIdentifier", rootIdentifier);

        // TODO do we want to return the actual Record type here?
        try {
            Record record = jdbcTemplate.queryForObject(
                    StringSubstitutor.replace(sql.getProperty("selectRecord"), tableMap),
                    new MapSqlParameterSource()
                            .addValue("localIdentifier", localIdentifier)
                            .addValue("expeditionId", result.expeditionId)
                            .addValue("conceptAlias", result.conceptAlias),
                    rowMappers.get(GenericRecord.class)
            );

            return new RecordResult(result.expeditionId, result.conceptAlias, record);
        } catch (EmptyResultDataAccessException e) {
            throw new FimsRuntimeException(QueryCode.NO_RESOURCES, 204);
        }
    }

    @Override
    public boolean delete(String rootIdentifier, String localIdentifier) {
        EntityIdentifierResult result = getEntityIdentifierResult(rootIdentifier);

        if (result == null) return false;

        Map<String, Object> tableMap = PostgresUtils.getTableMap(result.networkId, result.conceptAlias);
        tableMap.put("rootIdentifier", rootIdentifier);

        return jdbcTemplate.update(
                StringSubstitutor.replace(sql.getProperty("deleteRecord"), tableMap),
                new MapSqlParameterSource()
                        .addValue("identifier", localIdentifier)
                        .addValue("expeditionId", result.expeditionId)
        ) > 0;
    }

    private EntityIdentifierResult getEntityIdentifierResult(String rootIdentifier) {
        return jdbcTemplate.queryForObject(
                sql.getProperty("selectEntityIdentifier"),
                new MapSqlParameterSource().addValue("rootIdentifier", rootIdentifier),
                (rs, rowNum) -> {
                    EntityIdentifierResult r = new EntityIdentifierResult();
                    r.conceptAlias = rs.getString("conceptAlias");
                    r.expeditionId = rs.getInt("expeditionId");
                    r.networkId = rs.getInt("networkId");
                    return r;
                }
        );
    }

    @Override
    public List<? extends Record> getRecords(Project project, String conceptAlias, Class<? extends Record> recordType) {
        Map<String, Object> tableMap = PostgresUtils.getTableMap(project.getNetwork().getId(), conceptAlias);

        Map<String, Object> sqlParams = new HashMap<>();
        sqlParams.put("projectId", project.getProjectId());
        sqlParams.put("conceptAlias", conceptAlias);

        return jdbcTemplate.query(
                StringSubstitutor.replace(sql.getProperty("selectProjectRecords"), tableMap),
                sqlParams,
                rowMappers.getOrDefault(recordType, new GenericRecordRowMapper())
        );
    }

    @Override
    public List<? extends Record> getRecords(Project project, String expeditionCode, String conceptAlias, Class<? extends Record> recordType) {
        Map<String, Object> tableMap = PostgresUtils.getTableMap(project.getNetwork().getId(), conceptAlias);

        Map<String, Object> sqlParams = new HashMap<>();
        sqlParams.put("expeditionCode", expeditionCode);
        sqlParams.put("projectId", project.getProjectId());
        sqlParams.put("conceptAlias", conceptAlias);

        return jdbcTemplate.query(
                StringSubstitutor.replace(sql.getProperty("selectExpeditionRecords"), tableMap),
                sqlParams,
                rowMappers.getOrDefault(recordType, new GenericRecordRowMapper())
        );
    }

    @Override
    public List<? extends Record> getRecords(Project project, String expeditionCode, String conceptAlias, List<String> localIdentifiers, Class<? extends Record> recordType) {
        Map<String, Object> tableMap = PostgresUtils.getTableMap(project.getNetwork().getId(), conceptAlias);

        Map<String, Object> sqlParams = new HashMap<>();
        sqlParams.put("expeditionCode", expeditionCode);
        sqlParams.put("projectId", project.getProjectId());
        sqlParams.put("conceptAlias", conceptAlias);
        sqlParams.put("localIdentifiers", localIdentifiers);

        return jdbcTemplate.query(
                StringSubstitutor.replace(sql.getProperty("selectIndividualExpeditionRecords"), tableMap),
                sqlParams,
                rowMappers.getOrDefault(recordType, new GenericRecordRowMapper())
        );
    }

    @Override
    @SetFimsUser
    @SuppressWarnings({"unchecked"})
    public void saveChildRecord(Record record, int networkId, Entity parentEntity, Entity entity) {
        Map<String, String> extraValues = new HashMap<>();

        String parentIdentifier = record.get(parentEntity.getUniqueKeyURI());
        extraValues.put("parent_identifier", parentIdentifier);

        String s = sql.getProperty("insertChildRecord");
        save(record, networkId, entity, s, extraValues);
    }

    @Override
    @SetFimsUser
    @SuppressWarnings({"unchecked"})
    public void saveRecord(Record record, int networkId, Entity entity) {
        String s = sql.getProperty("insertRecord");
        save(record, networkId, entity, s, new HashMap<>());
    }

    private void save(Record record, int networkId, Entity entity, String sql, Map<String, String> extraValues) {
        String localIdentifierUri = entity.getUniqueKeyURI();
        Map<String, Object> tableMap = PostgresUtils.getTableMap(networkId, entity.getConceptAlias());

        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("expeditionCode", record.expeditionCode());
        params.addValue("projectId", record.projectId());
        String localIdentifier = record.get(localIdentifierUri);
        params.addValue("identifier", localIdentifier);

        params.addValues(extraValues);

        ObjectMapper mapper = new FimsObjectMapper();
        Map<String, Object> properties = record.properties();

        try {
            params.addValue("data", mapper.writeValueAsString(removeEmptyProperties(properties)));
        } catch (JsonProcessingException e) {
            throw new FimsRuntimeException(UploadCode.DATA_SERIALIZATION_ERROR, 500);
        }

        jdbcTemplate.update(
                StringSubstitutor.replace(sql, tableMap),
                params
        );
    }

    @Override
    @SetFimsUser
    @SuppressWarnings({"unchecked"})
    public void saveDataset(Dataset dataset, int networkId) {
        try {
            for (RecordSet recordSet : dataset) {

                String localIdentifierUri = recordSet.entity().getUniqueKeyURI();

                Map<String, Object> tableMap = PostgresUtils.getTableMap(networkId, recordSet.conceptAlias());

                List<HashMap<String, ?>> insertParams = new ArrayList<>();
                ObjectMapper mapper = new FimsObjectMapper();
                List<String> localIdentifiers = new ArrayList<>();
                List<String> parentIdentifiers = new ArrayList<>();

                for (Record record : recordSet.recordsToPersist()) {

                    HashMap<String, Object> recordParams = new HashMap<>();
                    recordParams.put("expeditionCode", recordSet.expeditionCode());
                    recordParams.put("projectId", recordSet.projectId());
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

                    Map<String, Object> properties = record.properties();
                    recordParams.put("data", mapper.writeValueAsString(removeEmptyProperties(properties)));

                    insertParams.add(recordParams);
                }

                if (insertParams.isEmpty() && !recordSet.reload()) continue;

                boolean executeReturning = false;
                String sqlString;
                if (recordSet.hasParent() && recordSet.parent().entity().isHashed()) {
                    sqlString = sql.getProperty("insertChildRecordReturning");
                    executeReturning = true;
                } else if (recordSet.hasParent()) {
                    sqlString = sql.getProperty("insertChildRecord");
                } else {
                    sqlString = sql.getProperty("insertRecord");
                }

                // used to remove hashed parents that have been updated and no longer have a child entity attached
                List<String> updatedHashedParents = new ArrayList<>();
                if (!insertParams.isEmpty()) {
                    if (executeReturning) {
                        for (HashMap p : insertParams) {
                            // we use query here b/c our update statement may return a value
                            List<String> old_parent = jdbcTemplate.query(
                                    StringSubstitutor.replace(sqlString, tableMap),
                                    p,
                                    (rs, rowNum) -> rs.getString("parent_identifier")
                            );
                            if (old_parent.size() == 1 && old_parent.get(0) != null) {
                                updatedHashedParents.add(old_parent.get(0));
                            } else if (old_parent.size() > 1) {
                                throw new FimsRuntimeException(QueryCode.UNEXPECTED_RESULT, 500);
                            }
                        }
                    } else {
                        jdbcTemplate.batchUpdate(
                                StringSubstitutor.replace(sqlString, tableMap),
                                insertParams.toArray(new HashMap[insertParams.size()])
                        );
                    }
                }

                // Delete any records not in the current RecordSet
                if (recordSet.reload()) {
                    String deleteSql;
                    HashMap<String, Object> deleteParams = new HashMap<>();
                    deleteParams.put("expeditionCode", recordSet.expeditionCode());
                    deleteParams.put("projectId", recordSet.projectId());

                    if (recordSet.hasParent()) {
                        deleteSql = sql.getProperty("deleteChildRecords");
                        List<Object[]> identifierTuples = new ArrayList<>(localIdentifiers.size());

                        for (int i = 0; i < localIdentifiers.size(); i++) {
                            identifierTuples.add(
                                    new Object[]{parentIdentifiers.get(i), localIdentifiers.get(i)}
                            );
                        }

                        if (!identifierTuples.isEmpty()) {
                            deleteSql += sql.getProperty("deleteChildRecordsIdentifierClause");
                            deleteParams.put("identifiers", identifierTuples);
                        }
                    } else {
                        deleteSql = sql.getProperty("deleteRecords");
                        if (!localIdentifiers.isEmpty()) {
                            deleteSql += sql.getProperty("deleteRecordsIdentifierClause");
                            deleteParams.put("identifiers", localIdentifiers);
                        }
                    }

                    jdbcTemplate.update(
                            StringSubstitutor.replace(deleteSql, tableMap),
                            deleteParams
                    );
                } else if (updatedHashedParents.size() > 0) {
                    String deleteSql = sql.getProperty("deleteOrphanedParentRecords");

                    HashMap<String, Object> deleteParams = new HashMap<>();
                    deleteParams.put("expeditionCode", recordSet.expeditionCode());
                    deleteParams.put("projectId", recordSet.projectId());
                    deleteParams.put("identifiers", updatedHashedParents);

                    tableMap.put("childTable", tableMap.get("table"));
                    tableMap.put("table", PostgresUtils.entityTable(networkId, recordSet.entity().getParentEntity()));

                    jdbcTemplate.update(
                            StringSubstitutor.replace(deleteSql, tableMap),
                            deleteParams
                    );
                }

            }
        } catch (JsonProcessingException e) {
            throw new FimsRuntimeException(UploadCode.DATA_SERIALIZATION_ERROR, 500);
        }
    }

    @Override
    public <T> List<T> query(String sql, SqlParameterSource params, Class<T> responseType) {
        return query(sql, params, (rs, rowNum) -> {
            ResultSetMetaData metadata = rs.getMetaData();
            Map<String, String> result = new HashMap<>();

            for (int i = 1; i <= metadata.getColumnCount(); i++) {
                // for some reason, the columnLabels are 1 indexed, not 0 indexed
                String label = metadata.getColumnLabel(i);
                result.put(label, rs.getString(label));
            }

            try {
                return JacksonUtil.fromMap(result, responseType);
            } catch (Exception e) {
                throw new SQLException(e);
            }
        });
    }

    @Override
    public <T> List<T> query(String sql, SqlParameterSource params, RowMapper<T> rowMapper) {
        logger.info(sql);
        return jdbcTemplate.query(sql, params, rowMapper);
    }

    @Override
    @SuppressWarnings({"unchecked"})
    public QueryResults query(Query query) {
        ParametrizedQuery q = query.parameterizedQuery();

        logger.info(q.toString());

        RecordRowCallbackHandler handler = new RecordRowCallbackHandler(query.configEntities());
        jdbcTemplate.query(q.sql(), q.params(), handler);

        return handler.results();
    }

    @Override
    @SuppressWarnings({"unchecked"})
    public PaginatedResponse<Map<String, List<Map<String, Object>>>> query(Query query, RecordSources sources, boolean includeEmptyProperties) {
        ParametrizedQuery q = query.parameterizedQuery();

        logger.info(q.toString());

        RecordRowCallbackHandler handler = new RecordRowCallbackHandler(query.configEntities());
        jdbcTemplate.query(q.sql(), q.params(), handler);

        QueryResults queryResults = handler.results();

        if (queryResults.isEmpty()) {
            return new PaginatedResponse<>(Collections.emptyMap(), 0, 0);
        }

        return new PaginatedResponse<>(queryResults.toMap(includeEmptyProperties, sources), query.page(), query.limit());
    }


    private Map<String, Object> removeEmptyProperties(Map<String, Object> properties) {
        Map<String, Object> nonEmptyProps = new HashMap<>();

        for (Map.Entry<String, Object> entry : properties.entrySet()) {
            Object value = entry.getValue();

            if (value == null || (value instanceof String && ((String) value).trim().equals(""))) {
                continue;
            }

            nonEmptyProps.put(entry.getKey(), value);
        }

        return nonEmptyProps;
    }

    private class RecordRowCallbackHandler implements RowCallbackHandler {
        private final List<Entity> entities;
        final Map<String, LinkedHashSet<Record>> records;
        final Map<String, String> rootIdentifiers;

        private RecordRowCallbackHandler(List<Entity> entities) {
            this.entities = entities;
            this.records = new HashMap<>();
            this.rootIdentifiers = new HashMap<>();
        }

        @Override
        public void processRow(ResultSet rs) throws SQLException {
            ResultSetMetaData metadata = rs.getMetaData();
            Map<String, Record> rowRecords = new LinkedHashMap<>();

            // process all columns in the row
            for (int i = 1; i <= metadata.getColumnCount(); i++) {
                // for some reason, the columnLabels are 1 indexed, not 0 indexed
                String label = metadata.getColumnLabel(i);

                if (label.endsWith("_data")) {
                    String conceptAlias = label.split("_data")[0].toLowerCase();
                    rowRecords.put(
                            conceptAlias,
                            getRowMapper(conceptAlias).mapRow(rs, 0, conceptAlias + "_")
                    );
                }
            }

            // add all records for the row to the records map
            for (Map.Entry<String, Record> entry : rowRecords.entrySet()) {
                String conceptAlias = entry.getKey();
                Record record = entry.getValue();

                if (record != null) {
                    // we use a Set for records b/c when selecting related entities, we use a LEFT JOIN
                    // b/c we want to return results even if there are not children. This causes 1
                    // parent (duplicate) for each child.
                    records.computeIfAbsent(conceptAlias, k -> new LinkedHashSet<>())
                            .add(record);
                }
            }
        }

        public QueryResults results() {
            List<QueryResult> results = new ArrayList<>();

            for (String conceptAlias : records.keySet()) {
                Entity e = getEntity(conceptAlias);
                Entity parentEntity = e.isChildEntity()
                        ? getEntity(e.getParentEntity())
                        : null;
                results.add(new QueryResult(new LinkedList<>(records.get(conceptAlias)), e, parentEntity, fimsProperties.bcidResolverPrefix()));
            }

            return new QueryResults(results);
        }

        private FimsRowMapper<? extends Record> getRowMapper(String conceptAlias) {
            FimsRowMapper<? extends Record> rowMapper = rowMappers.get(getEntity(conceptAlias).getRecordType());
            if (rowMapper == null) {
                rowMapper = rowMappers.get(GenericRecord.class);
            }

            return rowMapper;
        }

        private Entity getEntity(String conceptAlias) {
            for (Entity e : entities) {
                // conceptAlias's are not case-sensitive
                if (e.getConceptAlias().equalsIgnoreCase(conceptAlias)) return e;
            }
            throw new FimsRuntimeException(QueryCode.UNKNOWN_ENTITY, 500, conceptAlias);
        }
    }

    private class EntityIdentifierResult {
        public int networkId;
        public int expeditionId;
        public String conceptAlias;
    }
}
