package biocode.fims.rowMapper;

import biocode.fims.entities.Expedition;
import biocode.fims.entities.Project;
import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * {@link RowMapper} implementation mapping data from a {@link ResultSet} to the corresponding properties
 * of the {@link Expedition} class.
 */
public class ExpeditionRowMapper implements RowMapper<Expedition> {

    public Expedition mapRow(ResultSet rs, int rownum) throws SQLException {
        Project project =
                new Project.ProjectBuilder(
                        rs.getString("p.projectCode"),
                        rs.getString("p.projectTitle"),
                        rs.getInt("p.userId"),
                        rs.getString("p.validationXml"))
                .projectAbstract(rs.getString("p.abstract"))
                .isPublic(rs.getBoolean("p.public"))
                .build();

        project.setProjectId(rs.getInt("p.projectId"));
        project.setTs(rs.getTimestamp("p.ts"));

        Expedition.ExpeditionBuilder builder =
                new Expedition.ExpeditionBuilder(
                        rs.getString("e.expeditionCode"),
                        rs.getInt("e.userId"),
                        project
                )
                .expeditionTitle(rs.getString("e.expeditionTitle"))
                .isPublic(rs.getBoolean("e.public"));

        Expedition expedition = builder.build();
        expedition.setExpeditionId(rs.getInt("e.expeditionId"));
        expedition.setTs(rs.getTimestamp("e.ts"));
        return expedition;
    }
}
