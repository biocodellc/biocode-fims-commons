package biocode.fims.repositories;

import biocode.fims.fimsExceptions.FimsRuntimeException;
import biocode.fims.fimsExceptions.errorCodes.UploadCode;
import biocode.fims.models.records.GenericRecord;
import biocode.fims.models.records.GenericRecordRowMapper;
import biocode.fims.models.records.Record;
import biocode.fims.models.records.RecordSet;
import biocode.fims.query.PostgresUtils;
import biocode.fims.query.QueryResult;
import biocode.fims.query.dsl.Query;
import biocode.fims.rest.SpringObjectMapper;
import biocode.fims.run.Dataset;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.text.StrSubstitutor;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.sql.PreparedStatement;
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
                ObjectMapper mapper = new SpringObjectMapper();

                for (Record record : recordSet.recordsToPersist()) {

                    HashMap<String, Object> recordParams = new HashMap<>();
                    recordParams.put("expeditionId", expeditionId);
                    recordParams.put("identifier", record.get(localIdentifierUri));

                    if (recordSet.hasParent()) {
                        String parentEntityIdColumn = recordSet.parent().entity().getUniqueKey();
                        String parentIdentifierUri = recordSet.entity().getAttributeUri(parentEntityIdColumn);
                        recordParams.put("parent_identifier", record.get(parentIdentifierUri));
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

            }
        } catch (JsonProcessingException e) {
            throw new FimsRuntimeException(UploadCode.DATA_SERIALIZATION_ERROR, 500);
        }
    }

    @Override
    @SuppressWarnings({"unchecked"})
    public QueryResult query(Query query) {
        List<Record> records = (List<Record>) jdbcTemplate.query(query.query(), rowMappers.get(GenericRecord.class));

        return new QueryResult(records, query.entity());
    }

    @Override
    public void createEntityTable(int projectId, String conceptAlias) {
        createEntityTable(projectId, conceptAlias, Collections.emptyList());
    }

    @Override
    public void createEntityTable(int projectId, String conceptAlias, List<String> indexedColumnUris) {
        Map<String, Object> tableMap = getTableMap(projectId, conceptAlias);
        tableMap.put("conceptAlias", conceptAlias);
        tableMap.put("projectId", projectId);

        jdbcTemplate.execute(StrSubstitutor.replace(sql.getProperty("createEntityTable"), tableMap), PreparedStatement::execute);

        for (String column : indexedColumnUris) {
            createEntityTableIndex(projectId, conceptAlias, column);
        }
    }

    @Override
    public void createChildEntityTable(int projectId, String conceptAlias, String parentConceptAlias, String parentReferenceColumn) {
        createChildEntityTable(projectId, conceptAlias, parentConceptAlias, parentReferenceColumn, Collections.emptyList());
    }

    @Override
    public void createChildEntityTable(int projectId, String conceptAlias, String parentConceptAlias, String parentReferenceColumn, List<String> indexedColumnUris) {
        createEntityTable(projectId, conceptAlias, indexedColumnUris);

        Map<String, Object> paramMap = new HashMap<>();
        paramMap.put("table", PostgresUtils.entityTable(projectId, conceptAlias));
        paramMap.put("parentTable", PostgresUtils.entityTable(projectId, parentConceptAlias));
        paramMap.put("parentColumn", parentReferenceColumn);
        paramMap.put("conceptAlias", conceptAlias);
        paramMap.put("projectId", projectId);

        jdbcTemplate.execute(StrSubstitutor.replace(sql.getProperty("createChildEntityTableForeignKey"), paramMap), PreparedStatement::execute);
    }

    @Override
    public void createEntityTableIndex(int projectId, String conceptAlias, String column) {
        // TODO implement
    }

    @Override
    public void createProjectSchema(int projectId) {
        Map<String, Object> paramMap = new HashMap<>();
        paramMap.put("projectId", projectId);


        jdbcTemplate.execute(StrSubstitutor.replace(sql.getProperty("createProjectSchema"), paramMap), PreparedStatement::execute);
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
}
