package biocode.fims.repositories;

import biocode.fims.projectConfig.models.Entity;
import biocode.fims.fimsExceptions.FimsRuntimeException;
import biocode.fims.fimsExceptions.errorCodes.QueryCode;
import biocode.fims.fimsExceptions.errorCodes.UploadCode;
import biocode.fims.models.dataTypes.JacksonUtil;
import biocode.fims.models.records.*;
import biocode.fims.query.ParametrizedQuery;
import biocode.fims.query.PostgresUtils;
import biocode.fims.query.QueryResult;
import biocode.fims.query.QueryResults;
import biocode.fims.query.dsl.Query;
import biocode.fims.rest.FimsObjectMapper;
import biocode.fims.run.Dataset;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.text.StrSubstitutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
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

import static biocode.fims.bcid.Identifier.ROOT_IDENTIFIER;

/**
 * @author rjewing
 */
@Transactional
public class PostgresRecordRepository implements RecordRepository {
    private final static Logger logger = LoggerFactory.getLogger(PostgresRecordRepository.class);
    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final Properties sql;
    private final Map<Class<? extends Record>, FimsRowMapper<? extends Record>> rowMappers;

    public PostgresRecordRepository(NamedParameterJdbcTemplate jdbcTemplate, Properties sql, Map<Class<? extends Record>, FimsRowMapper<? extends Record>> rowMappers) {
        this.jdbcTemplate = jdbcTemplate;
        this.sql = sql;
        this.rowMappers = rowMappers;
    }

    @Override
    public RecordResult get(String rootIdentifier, String localIdentifier) {
        EntityIdentifierResult result = jdbcTemplate.queryForObject(
                sql.getProperty("selectEntityIdentifier"),
                new MapSqlParameterSource().addValue("rootIdentifier", rootIdentifier),
                (rs, rowNum) -> {
                    EntityIdentifierResult r = new EntityIdentifierResult();
                    r.conceptAlias = rs.getString("conceptAlias");
                    r.expeditionId = rs.getInt("expeditionId");
                    r.projectId = rs.getInt("projectId");
                    return r;
                }
        );

        if (result == null) return null;

        Map<String, Object> tableMap = getTableMap(result.projectId, result.conceptAlias);

        // TODO do we want to return the actual Record type here?
        try {
            Record record = jdbcTemplate.queryForObject(
                    StrSubstitutor.replace(sql.getProperty("selectRecord"), tableMap),
                    new MapSqlParameterSource()
                            .addValue("localIdentifier", localIdentifier)
                            .addValue("expeditionId", result.expeditionId),
                    rowMappers.get(GenericRecord.class)
            );
            record.set(ROOT_IDENTIFIER, rootIdentifier);

            return new RecordResult(result.projectId, result.expeditionId, result.conceptAlias, record);
        } catch (EmptyResultDataAccessException e) {
            throw new FimsRuntimeException(QueryCode.NO_RESOURCES, 204);
        }
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
    public void saveChildRecord(Record record, int projectId, Entity parentEntity, Entity entity, int expeditionId) {
        Map<String, String> extraValues = new HashMap<>();

        String parentIdentifier = record.get(parentEntity.getUniqueKeyURI());
        extraValues.put("parent_identifier", parentIdentifier);

        String s = sql.getProperty("insertChildRecord");
        save(record, projectId, entity, expeditionId, s, extraValues);
    }

    @Override
    @SetFimsUser
    @SuppressWarnings({"unchecked"})
    public void saveRecord(Record record, int projectId, Entity entity, int expeditionId) {
        String s = sql.getProperty("insertRecord");
        save(record, projectId, entity, expeditionId, s, new HashMap<>());

    }

    private void save(Record record, int projectId, Entity entity, int expeditionId, String sql, Map<String, String> extraValues) {
        String localIdentifierUri = entity.getUniqueKeyURI();
        Map<String, Object> tableMap = getTableMap(projectId, entity.getConceptAlias());

        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("expeditionId", expeditionId);
        String localIdentifier = record.get(localIdentifierUri);
        params.addValue("identifier", localIdentifier);

        params.addValues(extraValues);

        ObjectMapper mapper = new FimsObjectMapper();
        Map<String, String> properties = record.properties();

        try {
            params.addValue("data", mapper.writeValueAsString(removeEmptyProperties(properties)));
        } catch (JsonProcessingException e) {
            throw new FimsRuntimeException(UploadCode.DATA_SERIALIZATION_ERROR, 500);
        }

        jdbcTemplate.update(
                StrSubstitutor.replace(sql, tableMap),
                params
        );
    }

    @Override
    @SetFimsUser
    @SuppressWarnings({"unchecked"})
    public void saveDataset(Dataset dataset, int projectId, int expeditionId) {
        try {
            for (RecordSet recordSet : dataset) {

                String localIdentifierUri = recordSet.entity().getUniqueKeyURI();

                Map<String, Object> tableMap = getTableMap(projectId, recordSet.conceptAlias());

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
        boolean onlyPublicExpeditions = query.expeditions().isEmpty();
        ParametrizedQuery q = query.parameterizedQuery(onlyPublicExpeditions);

        logger.info(q.toString());

        RecordRowCallbackHandler handler = new RecordRowCallbackHandler(query.configEntities());
        jdbcTemplate.query(q.sql(), q.params(), handler);

        return handler.results();
    }

    @Override
    @SuppressWarnings({"unchecked"})
    public Page<Map<String, String>> query(Query query, int page, int limit, List<String> source, boolean includeEmptyProperties) {
        if (query.entities().size() > 1) {
            // TODO, remove this limitation. either implement pagination in psql query, or we need to
            // return a result which contains the current page for the queryEntity, and filter all
            // "related" entities from the current page
            throw new FimsRuntimeException(QueryCode.UNSUPPORTED_QUERY, 400);
        }
        // naive implementation that will work for now. Probably better to use postgres to paginate
        boolean onlyPublicExpeditions = query.expeditions().isEmpty();
        ParametrizedQuery q = query.parameterizedQuery(onlyPublicExpeditions);

        logger.info(q.toString());

        RecordRowCallbackHandler handler = new RecordRowCallbackHandler(query.configEntities());
        jdbcTemplate.query(q.sql(), q.params(), handler);

        QueryResults queryResults = handler.results();
        QueryResult queryResult = queryResults.getResult(query.queryEntity().getConceptAlias());

        int total = queryResult.records().size();
        int from = page * limit;
        int to = (from + limit < total) ? from + limit : total;

        if (to > queryResult.records().size()) {
            to = queryResult.records().size();
        }

        QueryResult result = new QueryResult(queryResult.records().subList(from, to), queryResult.entity());

        Pageable pageable = new PageRequest(page, limit);
        return new PageImpl<>(result.get(includeEmptyProperties, source), pageable, total);
    }


    private Map<String, Object> getTableMap(int projectId, String conceptAlias) {
        if (StringUtils.isBlank(conceptAlias)) {
            throw new IllegalStateException("entity conceptAlias must not be null");
        }

        Map<String, Object> tableMap = new HashMap<>();
        tableMap.put("table", PostgresUtils.entityTable(projectId, conceptAlias));
        return tableMap;
    }

    private Map<String, String> removeEmptyProperties(Map<String, String> properties) {
        Map<String, String> nonEmptyProps = new HashMap<>();

        for (Map.Entry<String, String> entry : properties.entrySet()) {
            String value = entry.getValue();

            if (value == null || value.trim().equals("")) {
                continue;
            }

            nonEmptyProps.put(entry.getKey(), value);
        }

        return nonEmptyProps;
    }

    private class RecordRowCallbackHandler implements RowCallbackHandler {
        private final List<Entity> entities;
        final Map<String, Set<Record>> records;
        final Map<String, String> rootIdentifiers;

        private RecordRowCallbackHandler(List<Entity> entities) {
            this.entities = entities;
            this.records = new HashMap<>();
            this.rootIdentifiers = new HashMap<>();
        }

        @Override
        public void processRow(ResultSet rs) throws SQLException {
            ResultSetMetaData metadata = rs.getMetaData();
            String rootIdentifier = null;
            String conceptAlias = null;
            Record record = null;

            int rowNum = 0;

            for (int i = 1; i <= metadata.getColumnCount(); i++) {
                // for some reason, the columnLabels are 1 indexed, not 0 indexed
                String label = metadata.getColumnLabel(i);

                if (label.endsWith("_root_identifier")) {
                    rootIdentifier = rs.getString(label);
                    rootIdentifiers.put(label.split("_root_identifier")[0].toLowerCase(), rs.getString(label));
                } else {
                    conceptAlias = label.split("_data")[0].toLowerCase();
                    record = getRowMapper(conceptAlias).mapRow(rs, rowNum, label);
                }

                if (rootIdentifier != null && record != null) {
                    record.set(ROOT_IDENTIFIER, rootIdentifier);
                    // we use a Set for records b/c when selecting related entities, we use a LEFT JOIN
                    // b/c we want to return results even if there are not children. This causes 1
                    // parent (duplicate) for each child.
                    records.computeIfAbsent(conceptAlias, k -> new HashSet<>())
                            .add(record);
                    rootIdentifier = null;
                    record = null;
                    conceptAlias = null;
                    rowNum++;
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
                results.add(new QueryResult(new ArrayList<>(records.get(conceptAlias)), e, parentEntity));
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
            throw new FimsRuntimeException(QueryCode.UNKNOWN_ENTITY, 500);
        }
    }

    private class EntityIdentifierResult {
        public int projectId;
        public int expeditionId;
        public String conceptAlias;
    }
}
