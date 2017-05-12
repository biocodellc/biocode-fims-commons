package biocode.fims.models.records;

import biocode.fims.models.dataTypes.JacksonUtil;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.type.TypeFactory;
import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

/**
 * @author rjewing
 */
public class GenericRecordRowMapper implements RowMapper<GenericRecord> {
    private final static JavaType TYPE;

    static {
        TYPE = TypeFactory.defaultInstance().constructMapType(HashMap.class, String.class, String.class);
    }

    @SuppressWarnings("unchecked")
    @Override
    public GenericRecord mapRow(ResultSet rs, int rowNum) throws SQLException {
        String data = rs.getString("data");

        try {
            Map<String, String> properties = (Map<String, String>) JacksonUtil.fromString(data, TYPE);
            return new GenericRecord(properties, false);
        } catch (Exception e) {
            throw new SQLException(e);
        }

    }
}
