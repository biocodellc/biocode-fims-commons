package biocode.fims.repositories;

import biocode.fims.digester.Entity;
import biocode.fims.models.dataTypes.JacksonUtil;
import biocode.fims.projectConfig.ProjectConfig;
import biocode.fims.query.PostgresUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.text.StrSubstitutor;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.sql.PreparedStatement;
import java.util.*;

/**
 * @author rjewing
 */
@Transactional
public class PostgresProjectConfigRepository implements ProjectConfigRepository {
    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final Properties sql;

    public PostgresProjectConfigRepository(NamedParameterJdbcTemplate jdbcTemplate, Properties sql) {
        this.jdbcTemplate = jdbcTemplate;
        this.sql = sql;
    }

    @Override
    public ProjectConfig getConfig(int projectId) {
        Map<String, Object> sqlParams = new HashMap<>();
        sqlParams.put("projectId", projectId);

        return jdbcTemplate.queryForObject(
                sql.getProperty("getConfig"),
                sqlParams,
                (rs, rowNum) -> JacksonUtil.fromString(rs.getString("config"), ProjectConfig.class)
        );
    }

    @Override
    @SetFimsUser
    public void createProjectSchema(int projectId) {
        Map<String, Object> paramMap = new HashMap<>();
        paramMap.put("projectId", projectId);

        jdbcTemplate.execute(StrSubstitutor.replace(sql.getProperty("createProjectSchema"), paramMap), PreparedStatement::execute);
    }

    @Override
    public void createEntityTables(List<Entity> entities, int projectId, ProjectConfig config) {
        if (entities.size() == 0) {
            return;
        }

        StringBuilder entityTableSql = new StringBuilder();

        for (Entity e : sortEntities(entities)) {
            entityTableSql.append((createEntityTableSql(e, projectId, config)));
        }

        jdbcTemplate.execute(entityTableSql.toString(), PreparedStatement::execute);
    }

    @Override
    public void removeEntityTables(List<Entity> entities, int projectId) {
        if (entities.size() == 0) {
            return;
        }

        StringBuilder entityTableSql = new StringBuilder();

        // need to drop any child tables before the parent tables
        List<Entity> entitiesToRemove = sortEntities(entities);
        Collections.reverse(entitiesToRemove);
        for (Entity e : entitiesToRemove) {
            entityTableSql.append("DROP TABLE ")
                    .append(PostgresUtils.entityTable(projectId, e.getConceptAlias()))
                    .append(";");
        }

        jdbcTemplate.execute(entityTableSql.toString(), PreparedStatement::execute);
    }

    @Override
    @SetFimsUser
    public void save(ProjectConfig config, int projectId) {
        Map<String, Object> sqlParams = new HashMap<>();
        sqlParams.put("projectId", projectId);
        sqlParams.put("config", JacksonUtil.toString(config));

        jdbcTemplate.update(
                sql.getProperty("updateConfig"),
                sqlParams
        );
    }

    private String createEntityTableSql(Entity e, int projectId, ProjectConfig config) {
        String createSql;

        Map<String, Object> paramMap = new HashMap<>();
        paramMap.put("table", PostgresUtils.entityTable(projectId, e.getConceptAlias()));
        paramMap.put("conceptAlias", e.getConceptAlias());
        paramMap.put("projectId", projectId);

        createSql = StrSubstitutor.replace(sql.getProperty("createEntityTable"), paramMap);

        if (e.isChildEntity()) {
            paramMap.put("parentTable", PostgresUtils.entityTable(projectId, e.getParentEntity()));
            paramMap.put("parentColumn", e.getAttributeUri(config.entity(e.getParentEntity()).getUniqueKey()));
            createSql += StrSubstitutor.replace(sql.getProperty("createChildEntityTableForeignKey"), paramMap);
        }

        return createSql;
    }

    private LinkedList<Entity> sortEntities(List<Entity> entities) {
        LinkedList<Entity> sortedEntites = new LinkedList<>();

        for (Entity e : entities) {
            if (StringUtils.isBlank(e.getParentEntity())) {
                sortedEntites.add(0, e);
            } else {
                int index = 0;
                for (Entity sortedEntity : sortedEntites) {
                    index++;
                    if (StringUtils.isBlank(sortedEntity.getParentEntity())) {
                        continue;
                    } else if (e.getParentEntity().equals(sortedEntity.getConceptAlias())) {
                        break;
                    }
                }
                sortedEntites.add(index, e);
            }
        }

        return sortedEntites;
    }
}
