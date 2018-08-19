package biocode.fims.records;

import biocode.fims.models.dataTypes.JacksonUtil;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.type.TypeFactory;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

import static biocode.fims.query.QueryConstants.*;

/**
 * @author rjewing
 */
public class GenericRecordRowMapper implements FimsRowMapper<GenericRecord> {
    private final static JavaType TYPE;

    static {
        TYPE = TypeFactory.defaultInstance().constructMapType(HashMap.class, String.class, String.class);
    }

    @SuppressWarnings("unchecked")
    @Override
    public GenericRecord mapRow(ResultSet rs, int rowNum, String labelPrefix) throws SQLException {
        String data = rs.getString(labelPrefix + DATA);
        // data may be null when query includes child records
        if (data == null) return null;

        String rootIdentifier = rs.getString(labelPrefix + ROOT_IDENTIFIER);
        String expeditionCode = rs.getString(EXPEDITION_CODE.toString());
        int projectId = rs.getInt(PROJECT_ID.toString());

        try {
            Map<String, String> properties = (Map<String, String>) JacksonUtil.fromString(data, TYPE);
            return new GenericRecord(properties, rootIdentifier, projectId, expeditionCode, false);
        } catch (Exception e) {
            throw new SQLException(e);
        }

    }

    @Override
    public GenericRecord mapRow(ResultSet rs, int rowNum) throws SQLException {
        return mapRow(rs, rowNum, "");
    }
}
