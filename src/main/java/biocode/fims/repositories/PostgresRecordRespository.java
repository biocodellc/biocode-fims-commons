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
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author rjewing
 */
@Transactional
public class PostgresRecordRespository implements RecordRepository {
    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final Map<Class<? extends Record>, RowMapper<? extends Record>> rowMappers;

    private final static String INSERT_SQL = "INSERT INTO ${schema}.${table} (expedition_id, local_identifier, data) " +
            "VALUES (:expeditionId, :identifier, to_jsonb(:data::jsonb)) " +
            "ON CONFLICT (local_identifier, expedition_id) " +
            "DO UPDATE SET data = to_jsonb(:data::jsonb)";

    private final static String SCHEMA_SQL = "SELECT projectCode from projects where projectId = :projectId";

    private final static String SELECT_RECORDS_SQL = "SELECT r.data as data from ${schema}.${table} AS r " +
            "INNER JOIN expeditions e on r.expediton_id = e.expeditionid " +
            "WHERE e.expeditionCode = :expeditionCode AND e.projectId = :projectId";

    public PostgresRecordRespository(NamedParameterJdbcTemplate jdbcTemplate, Map<Class<? extends Record>, RowMapper<? extends Record>> rowMappers) {
        this.jdbcTemplate = jdbcTemplate;
        this.rowMappers = rowMappers;
    }

    @Override
    public List<? extends Record> getRecords(int projectId, String expeditionCode, String conceptAlias, Class<? extends Record> recordType) {
        Map<String, String> tableMap = new HashMap<>();
        tableMap.put("schema", getSchema(projectId));
        tableMap.put("table", conceptAlias);

        Map<String, Object> sqlParams = new HashMap<>();
        sqlParams.put("expeditionCode", expeditionCode);
        sqlParams.put("projectId", projectId);

        return jdbcTemplate.query(
                StrSubstitutor.replace(SELECT_RECORDS_SQL, tableMap),
                sqlParams,
                rowMappers.getOrDefault(recordType, new GenericRecordRowMapper())
        );
    }

    private String getSchema(int projectId) {
        return jdbcTemplate.queryForObject(SCHEMA_SQL, new MapSqlParameterSource("projectId", projectId), String.class);
    }

    @Override
    @SuppressWarnings({"unchecked"})
    public void save(List<RecordSet> recordSets, String projectCode, int expeditionId) {
        try {
            for (RecordSet recordSet : recordSets) {

                String table = recordSet.conceptAlias();
                String localIdentifierUri = recordSet.entity().getUniqueKeyURI();

                if (StringUtils.isBlank(table)) {
                    throw new IllegalStateException("entity conceptAlias must not be null");
                }

                Map<String, String> tableMap = new HashMap<>();
                tableMap.put("schema", projectCode);
                tableMap.put("table", table);

                List<HashMap<String, ?>> insertParams = new ArrayList<>();
                ObjectMapper mapper = new SpringObjectMapper();

                for (Record record : recordSet.recordsToPersist()) {

                    HashMap<String, Object> recordParams = new HashMap<>();
                    recordParams.put("schema", projectCode);
                    recordParams.put("table", table);
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
