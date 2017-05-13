package biocode.fims.repositories;

import biocode.fims.fimsExceptions.FimsRuntimeException;
import biocode.fims.fimsExceptions.errorCodes.UploadCode;
import biocode.fims.models.records.GenericRecordRowMapper;
import biocode.fims.models.records.Record;
import biocode.fims.models.records.RecordSet;
import biocode.fims.rest.SpringObjectMapper;
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
    private final Map<Class<? extends Record>, RowMapper<? extends Record>> rowMappers;

    private final static String INSERT_SQL = "INSERT INTO project_${projectId}.${table} (expedition_id, local_identifier, data) " +
            "VALUES (:expeditionId, :identifier, to_jsonb(:data::jsonb)) " +
            "ON CONFLICT (local_identifier, expedition_id) " +
            "DO UPDATE SET data = to_jsonb(:data::jsonb)";

    private final static String SELECT_RECORDS_SQL = "SELECT r.data as data from project_${projectId}.${table} AS r " +
            "INNER JOIN expeditions e on r.expediton_id = e.expeditionid " +
            "WHERE e.expeditionCode = :expeditionCode AND e.projectId = :projectId";

    private final static String CREATE_ENTITY_TABLE_SQL = "CREATE TABLE project_${projectId}.${table} " +
            "(id SERIAL PRIMARY KEY, local_identifier TEXT NOT NULL, expedition_id INT NOT NULL REFERENCES expeditions (expeditionId) ON DELETE CASCADE, " +
            "data JSONB NOT NULL, tsv TSVECTOR, " +
            "UNIQUE (local_identifier, expedition_id))";

    private final static String ENTITY_TABLE_TSV_TRIGGER_SQL = "CREATE TRIGGER tsvectorupdate " +
            "BEFORE INSERT OR UPDATE ON project_${projectId}.${table} " +
            "FOR EACH ROW EXECUTE PROCEDURE entity_tsv_trigger()";

    private final static String ENTITY_DATA_GIN_INDEX_SQL = "CREATE INDEX idx_project_${projectId}_${table}_data ON project_${projectId}.${table} USING GIN (data)";
    private final static String ENTITY_TSV_GIN_INDEX_SQL = "CREATE INDEX idx_project_${projectId}_${table}_tsv ON project_${projectId}.${table} USING GIN (tsv)";

    public PostgresRecordRepository(NamedParameterJdbcTemplate jdbcTemplate, Map<Class<? extends Record>, RowMapper<? extends Record>> rowMappers) {
        this.jdbcTemplate = jdbcTemplate;
        this.rowMappers = rowMappers;
    }

    @Override
    public List<? extends Record> getRecords(int projectId, String expeditionCode, String conceptAlias, Class<? extends Record> recordType) {
        Map<String, Object> tableMap = getTableMap(projectId, conceptAlias);

        Map<String, Object> sqlParams = new HashMap<>();
        sqlParams.put("expeditionCode", expeditionCode);
        sqlParams.put("projectId", projectId);

        return jdbcTemplate.query(
                StrSubstitutor.replace(SELECT_RECORDS_SQL, tableMap),
                sqlParams,
                rowMappers.getOrDefault(recordType, new GenericRecordRowMapper())
        );
    }

    @Override
    @SuppressWarnings({"unchecked"})
    public void save(List<RecordSet> recordSets, int projectId, int expeditionId) {
        try {
            for (RecordSet recordSet : recordSets) {

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

                    Map<String, String> properties = record.properties();
                    recordParams.put("data", mapper.writeValueAsString(removeEmptyProperties(properties)));

                    insertParams.add(recordParams);
                }

                jdbcTemplate.batchUpdate(StrSubstitutor.replace(INSERT_SQL, tableMap), insertParams.toArray(new HashMap[insertParams.size()]));

            }
        } catch (JsonProcessingException e) {
            throw new FimsRuntimeException(UploadCode.DATA_SERIALIZATION_ERROR, 500);
        }
    }

    @Override
    public void createEntityTable(int projectId, String conceptAlias) {
        createEntityTable(projectId, conceptAlias, Collections.emptyList());
    }

    @Override
    public void createEntityTable(int projectId, String conceptAlias, List<String> indexedColumnUris) {
        Map<String, Object> tableMap = getTableMap(projectId, conceptAlias);

        jdbcTemplate.execute(StrSubstitutor.replace(CREATE_ENTITY_TABLE_SQL, tableMap), PreparedStatement::execute);

        jdbcTemplate.execute(StrSubstitutor.replace(ENTITY_TABLE_TSV_TRIGGER_SQL, tableMap), PreparedStatement::execute);

        jdbcTemplate.execute(StrSubstitutor.replace(ENTITY_DATA_GIN_INDEX_SQL, tableMap), PreparedStatement::execute);
        jdbcTemplate.execute(StrSubstitutor.replace(ENTITY_TSV_GIN_INDEX_SQL, tableMap), PreparedStatement::execute);

        for (String column: indexedColumnUris) {
            createEntityTableIndex(projectId, conceptAlias, column);
        }
    }

    @Override
    public void createEntityTableIndex(int projectId, String conceptAlias, String column) {
        // TODO implement
    }

    private Map<String, Object> getTableMap(int projectId, String table) {
        Map<String, Object> tableMap = new HashMap<>();
        tableMap.put("projectId", projectId);
        tableMap.put("table", table);
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
