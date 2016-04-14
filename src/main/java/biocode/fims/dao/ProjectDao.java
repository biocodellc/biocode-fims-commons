package biocode.fims.dao;

import biocode.fims.entities.Project;
import biocode.fims.rowMapper.ProjectRowMapper;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.simple.SimpleJdbcInsert;

import javax.sql.DataSource;
import java.sql.Timestamp;
import java.util.Date;

public class ProjectDao {

    private NamedParameterJdbcTemplate namedParameterJdbcTemplate;

    private SimpleJdbcInsert insertProject;

    public ProjectDao(DataSource dataSource) {
        this.namedParameterJdbcTemplate = new NamedParameterJdbcTemplate(dataSource);

        this.insertProject = new SimpleJdbcInsert(dataSource)
                .withTableName("projects")
                .usingGeneratedKeyColumns("projectId");

    }

    public void update(Project project) {
        String updateTemplate = "UPDATE projects SET projectCode=:=projectCode, projectTitle=:projectTitle, " +
                "abstract=:abstract, ts=:ts, validationXml=:validationXml, userId=:userId, public=:public " +
                "WHERE projectId=:projectId";

        this.namedParameterJdbcTemplate.update(
                updateTemplate,
                createProjectParameterSource(project));
    }

    public void create(Project project) {

        project.setProjectId(
                this.insertProject.executeAndReturnKey(
                    createProjectParameterSource(project)
                ).intValue()
        );
    }

    public Project findProject(MapSqlParameterSource params) throws EmptyResultDataAccessException {
        StringBuilder selectStringBuilder = new StringBuilder(
                "SELECT projectId, projectCode, projectTitle, abstract, ts, validationXml, " +
                        "userId, public FROM projects WHERE ");

        int cnt = 0;
        for (String key: params.getValues().keySet()) {
            if (cnt > 0)
                selectStringBuilder.append(" and ");

            selectStringBuilder.append(key);
            selectStringBuilder.append("=:");
            selectStringBuilder.append(key);

            cnt++;
        }

        return this.namedParameterJdbcTemplate.queryForObject(
                selectStringBuilder.toString(),
                params,
                new ProjectRowMapper()
        );
    }

    /**
     * Creates a {@link MapSqlParameterSource} based on data values from the supplied {@link Project} instance.
     */
    private MapSqlParameterSource createProjectParameterSource(Project project) {
        return new MapSqlParameterSource()
                .addValue("projectId", project.getProjectId())
                .addValue("projectCode", project.getProjectCode())
                .addValue("projectTitle", project.getProjectTitle())
                .addValue("abstract", project.getProjectAbstract())
                .addValue("ts", new Timestamp(new Date().getTime()))
                .addValue("validationXml", project.getValidationXml())
                .addValue("userId", project.getUserId())
                .addValue("public", project.isPublic());
    }


}
