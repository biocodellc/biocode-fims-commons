package biocode.fims.models.records;

import biocode.fims.models.dataTypes.JacksonUtil;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.type.TypeFactory;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

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
    public GenericRecord mapRow(ResultSet rs, int rowNum, String dataLabel) throws SQLException {
        String data = rs.getString(dataLabel);
        // data may be null when query includes child records
        if (data == null) return null;

        try {
            Map<String, String> properties = (Map<String, String>) JacksonUtil.fromString(data, TYPE);
            return new GenericRecord(properties, false);
        } catch (Exception e) {
            throw new SQLException(e);
        }

    }

    @Override
    public GenericRecord mapRow(ResultSet rs, int rowNum) throws SQLException {
        return mapRow(rs, rowNum, "data");
    }
}
