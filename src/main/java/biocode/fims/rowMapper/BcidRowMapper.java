package biocode.fims.rowMapper;

import biocode.fims.entities.Bcid;
import biocode.fims.fimsExceptions.ServerErrorException;
import org.springframework.jdbc.core.RowMapper;

import java.net.URI;
import java.net.URISyntaxException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;

/**
 * {@link RowMapper} implementation mapping data from a {@link ResultSet} to the corresponding properties
 * of the {@link Bcid} class.
 */
public class BcidRowMapper implements RowMapper<Bcid> {

    public Bcid mapRow(ResultSet rs, int rownum) throws SQLException {
        try {
            Bcid.BcidBuilder builder =
                    new Bcid.BcidBuilder(rs.getInt("userId"), rs.getString("resourceType"))
                            .ezidMade(rs.getBoolean("ezidMade"))
                            .ezidRequest(rs.getBoolean("ezidRequest"))
                            .suffixPassThrough(rs.getBoolean("suffixPassThrough"))
                            .internalId(UUID.fromString(rs.getString("internalId")))
                            .identifier(URI.create(rs.getString("identifier")))
                            .doi(rs.getString("doi"))
                            .title(rs.getString("title"))
                            .webAddress((rs.getURL("webAddress") != null) ? rs.getURL("webAddress").toURI() : null)
                            .graph(rs.getString("graph"))
                            .finalCopy(rs.getBoolean("finalCopy"));

            Bcid bcid = builder.build();
            bcid.setBcidId(rs.getInt("bcidId"));
            bcid.setTs(rs.getTimestamp("ts"));

            return bcid;
        } catch (URISyntaxException e) {
            throw new ServerErrorException("Invalid webAddress for bcid: " + rs.getInt("bcidId"), e);
        }
    }
}
