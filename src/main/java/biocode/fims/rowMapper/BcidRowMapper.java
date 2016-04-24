package biocode.fims.rowMapper;

import biocode.fims.entities.Bcid;
import org.springframework.jdbc.core.RowMapper;

import java.net.URI;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * {@link RowMapper} implementation mapping data from a {@link ResultSet} to the corresponding properties
 * of the {@link Bcid} class.
 */
public class BcidRowMapper implements RowMapper<Bcid> {

    public Bcid mapRow(ResultSet rs, int rownum) throws SQLException {
        Bcid.BcidBuilder builder =
                new Bcid.BcidBuilder(rs.getInt("userId"), rs.getString("resourceType"))
                        .ezidMade(rs.getBoolean("ezidMade"))
                        .ezidRequest(rs.getBoolean("ezidRequest"))
                        .identifier(URI.create(rs.getString("identifier")))
                        .doi(rs.getString("doi"))
                        .title(rs.getString("title"))
                        .webAddress((rs.getString("webAddress") != null) ? URI.create(rs.getString("webAddress")) : null)
                        .graph(rs.getString("graph"))
                        .finalCopy(rs.getBoolean("finalCopy"));

        Bcid bcid = builder.build();
        bcid.setBcidId(rs.getInt("bcidId"));
        bcid.setTs(rs.getTimestamp("ts"));

        return bcid;
    }
}
