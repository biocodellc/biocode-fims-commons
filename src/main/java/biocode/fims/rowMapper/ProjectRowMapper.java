package biocode.fims.rowMapper;

import biocode.fims.entities.Project;
import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * {@link RowMapper} implementation mapping data from a {@link ResultSet} to the corresponding properties
 * of the {@link Project} class.
 */
public class ProjectRowMapper implements RowMapper<Project> {

    public Project mapRow(ResultSet rs, int rownum) throws SQLException {
        Project.ProjectBuilder builder =
                new Project.ProjectBuilder(
                        rs.getString("projectCode"),
                        rs.getString("projectTitle"),
                        rs.getInt("userId"),
                        rs.getString("validationXml"))
                .projectAbstract(rs.getString("abstract"))
                .isPublic(rs.getBoolean("public"));

        Project project = builder.build();
        project.setProjectId(rs.getInt("projectId"));
        project.setTs(rs.getTimestamp("ts"));

        return project;
    }
}
