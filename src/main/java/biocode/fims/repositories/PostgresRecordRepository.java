package biocode.fims.repositories;

import biocode.fims.digester.Entity;
import biocode.fims.fimsExceptions.FimsRuntimeException;
import biocode.fims.fimsExceptions.errorCodes.QueryCode;
import biocode.fims.fimsExceptions.errorCodes.UploadCode;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
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
    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final Properties sql;
    private final Map<Class<? extends Record>, FimsRowMapper<? extends Record>> rowMappers;

    public PostgresRecordRepository(NamedParameterJdbcTemplate jdbcTemplate, Properties sql, Map<Class<? extends Record>, FimsRowMapper<? extends Record>> rowMappers) {
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
    public QueryResults query(Query query) {
        boolean onlyPublicExpeditions = query.expeditions().isEmpty();
        ParametrizedQuery q = query.parameterizedQuery(onlyPublicExpeditions);
        RecordRowCallbackHandler handler = new RecordRowCallbackHandler(new ArrayList<>(query.entities()));
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

        RecordRowCallbackHandler handler = new RecordRowCallbackHandler(new ArrayList(query.entities()));
        jdbcTemplate.query(q.sql(), q.params(), handler);

        QueryResults queryResults = handler.results();
        QueryResult queryResult = queryResults.getResult(query.queryEntity().getConceptAlias());

        int total = queryResult.records().size();
        int from = page * limit;
        int to = (from + limit < total) ? from + limit : total;

        if (to > queryResult.records().size()) {
            to = queryResult.records().size();
        }

        QueryResult result = new QueryResult(queryResult.records().subList(from, to), queryResult.entity(), queryResult.rootIdentifier());

        Pageable pageable = new PageRequest(page, limit);
        return new PageImpl<>(result.get(includeEmptyProperties, source), pageable, total);
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
        private final List<Entity> entities;
        final Map<String, List<Record>> records;
        final Map<String, String> rootIdentifiers;

        private RecordRowCallbackHandler(List<Entity> entities) {
            this.entities = entities;
            this.records = new HashMap<>();
            this.rootIdentifiers = new HashMap<>();
        }

        @Override
        public void processRow(ResultSet rs) throws SQLException {
            // TODO is it possible to get determine the column labels here?
            // if so, we can build a query that will allow us to select ancestor data at the same time
            // would need to update queryBuilder to generate the correct select statement
            // and would need to pass in a list of entities to this class
            // we can select data & root_identifier for each queryEntity
            // labels can be something like alias.root_identifier and alias.data
            ResultSetMetaData metadata = rs.getMetaData();
            for (int i = 1; i <= metadata.getColumnCount(); i++) {
                // for some reason, the columnLabels are 1 indexed, not 0 indexed
                String label = metadata.getColumnLabel(i);

                if (label.endsWith("_root_identifier")) {
                    rootIdentifiers.put(label.split("_root_identifier")[0], rs.getString(label));
                } else {
                    String conceptAlias = label.split("_data")[0];

                    records.computeIfAbsent(conceptAlias, k -> new ArrayList<>())
                            .add(getRowMapper(conceptAlias).mapRow(rs, records.get(conceptAlias).size() - 1, label));
                }
            }
        }

        public QueryResults results() {
            List<QueryResult> results = new ArrayList<>();

            for (String conceptAlias : records.keySet()) {
                results.add(new QueryResult(records.get(conceptAlias), getEntity(conceptAlias), rootIdentifiers.get(conceptAlias)));
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
}
