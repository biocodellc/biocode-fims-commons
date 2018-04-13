package biocode.fims.models.records;

import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * @author rjewing
 */
public interface FimsRowMapper<T> extends RowMapper<T> {

    T mapRow(ResultSet rs, int rowNum, String dataLabel) throws SQLException;
}