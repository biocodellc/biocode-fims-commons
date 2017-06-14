package biocode.fims.repositories;

import biocode.fims.digester.Entity;
import biocode.fims.fimsExceptions.FimsRuntimeException;
import biocode.fims.fimsExceptions.errorCodes.ConfigCode;
import biocode.fims.models.Expedition;
import biocode.fims.models.dataTypes.JacksonUtil;
import biocode.fims.projectConfig.ProjectConfig;
import biocode.fims.projectConfig.ProjectConfigUpdator;
import biocode.fims.query.PostgresUtils;
import biocode.fims.service.ExpeditionService;
import biocode.fims.settings.SettingsManager;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.text.StrSubstitutor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.sql.PreparedStatement;
import java.util.*;

/**
 * @author rjewing
 */
@Transactional
public class PostgresProjectConfigRepository implements ProjectConfigRepository {
    private final JdbcTemplate jdbcTemplate;
    private final Properties sql;
    private final SettingsManager settingsManager;
    private final ExpeditionService expeditionService;

    public PostgresProjectConfigRepository(JdbcTemplate jdbcTemplate, Properties sql, SettingsManager settingsManager,
                                           ExpeditionService expeditionService) {
        this.jdbcTemplate = jdbcTemplate;
        this.sql = sql;
        this.settingsManager = settingsManager;
        this.expeditionService = expeditionService;
    }

    @Override
    public void createProjectSchema(int projectId) {
        Map<String, Object> paramMap = new HashMap<>();
        paramMap.put("projectId", projectId);

        jdbcTemplate.execute(StrSubstitutor.replace(sql.getProperty("createProjectSchema"), paramMap));
    }

    @Override
    public void save(ProjectConfig config, int projectId) {
        save(config, projectId, false);
    }

    @Override
    public void save(ProjectConfig config, int projectId, boolean checkForExistingBcids) {
        config.generateUris();

        if (!config.isValid()) {
            throw new FimsRuntimeException(ConfigCode.INVALID, 400);
        }

        ProjectConfig existingConfig = jdbcTemplate.queryForObject(
                sql.getProperty("getConfig"),
                new Object[]{projectId},
                (rs, rowNum) -> JacksonUtil.fromString(rs.getString("config"), ProjectConfig.class)
        );

        if (existingConfig != null) {
            config = updateConfig(config, projectId, existingConfig, checkForExistingBcids);
        }

        jdbcTemplate.update(
                sql.getProperty("updateConfig"),
                JacksonUtil.toString(config)
        );
    }

    private ProjectConfig updateConfig(ProjectConfig config, int projectId, ProjectConfig existingConfig, boolean checkForExistingBcids) {
        ProjectConfigUpdator updator = new ProjectConfigUpdator(config);
        config = updator.update(existingConfig);

        List<String> entityTableSqlStatements = new ArrayList<>();

        for (Entity e: sortEntities(updator.newEntities())) {
            entityTableSqlStatements.add(createEntityTableSql(e, projectId, config));
        }

        // need to drop any child tables before the parent tables
        List<Entity> entitiesToRemove = sortEntities(updator.removedEntities());
        Collections.reverse(entitiesToRemove);
        for (Entity e: entitiesToRemove) {
            entityTableSqlStatements.add(
                    "DROP TABLE " + PostgresUtils.entityTable(projectId, e.getConceptAlias() + ";")
            );
        }

        jdbcTemplate.batchUpdate(entityTableSqlStatements.toArray(new String[] {}));

        createEntityBcids(updator.newEntities(), projectId, checkForExistingBcids);

        return config;
    }

    private void createEntityBcids(List<Entity> entities, int projectId, boolean checkForExistingBcids) {
        boolean ezidRequest = Boolean.parseBoolean(settingsManager.retrieveValue("ezidRequests"));

        for (Expedition e: expeditionService.getExpeditions(projectId, true)) {
            expeditionService.createEntityBcids(entities, e.getExpeditionId(), e.getUser().getUserId(), ezidRequest, checkForExistingBcids);
        }
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

        for (Entity e: entities) {
            if (StringUtils.isBlank(e.getParentEntity())) {
                sortedEntites.add(0, e);
            } else {
                int index = 0;
                for (Entity sortedEntity: sortedEntites) {
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
