package biocode.fims.rowMapper;

import biocode.fims.entities.Expedition;
import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * {@link RowMapper} implementation mapping data from a {@link ResultSet} to the corresponding properties
 * of the {@link Expedition} class.
 */
public class ExpeditionRowMapper implements RowMapper<Expedition> {

    public Expedition mapRow(ResultSet rs, int rownum) throws SQLException {
        Expedition.ExpeditionBuilder builder =
                new Expedition.ExpeditionBuilder(
                        rs.getString("expeditionCode"),
                        rs.getInt("userId"),
                        rs.getInt("projectId")
                )
                .expeditionTitle(rs.getString("expeditionTitle"))
                .isPublic(rs.getBoolean("public"));

        Expedition expedition = builder.build();
        expedition.setExpeditionId(rs.getInt("expeditionId"));
        expedition.setTs(rs.getTimestamp("ts"));
        return expedition;
    }
}
