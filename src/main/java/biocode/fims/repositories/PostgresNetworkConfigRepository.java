package biocode.fims.repositories;

import biocode.fims.config.models.Entity;
import biocode.fims.config.network.NetworkConfig;
import biocode.fims.models.dataTypes.JacksonUtil;
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
public class PostgresNetworkConfigRepository implements NetworkConfigRepository {
    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final Properties sql;

    public PostgresNetworkConfigRepository(NamedParameterJdbcTemplate jdbcTemplate, Properties sql) {
        this.jdbcTemplate = jdbcTemplate;
        this.sql = sql;
    }

    @Override
    public NetworkConfig getConfig(int networkId) {
        Map<String, Object> sqlParams = new HashMap<>();
        sqlParams.put("networkId", networkId);

        return jdbcTemplate.queryForObject(
                sql.getProperty("getConfig"),
                sqlParams,
                (rs, rowNum) -> JacksonUtil.fromString(rs.getString("config"), NetworkConfig.class)
        );
    }

    @Override
    @SetFimsUser
    public void createNetworkSchema(int networkId) {
        Map<String, Object> paramMap = new HashMap<>();
        paramMap.put("networkId", networkId);

        jdbcTemplate.execute(StrSubstitutor.replace(sql.getProperty("createNetworkSchema"), paramMap), PreparedStatement::execute);
    }

    @Override
    public void createEntityTables(List<Entity> entities, int networkId, NetworkConfig config) {
        if (entities.size() == 0) {
            return;
        }

        StringBuilder entityTableSql = new StringBuilder();

        for (Entity e : sortEntities(entities)) {
            entityTableSql.append((createEntityTableSql(e, networkId, config)));
        }

        jdbcTemplate.execute(entityTableSql.toString(), PreparedStatement::execute);
    }

    @Override
    public void removeEntityTables(List<Entity> entities, int networkId) {
        if (entities.size() == 0) {
            return;
        }

        StringBuilder entityTableSql = new StringBuilder();

        // need to drop any child tables before the parent tables
        List<Entity> entitiesToRemove = sortEntities(entities);
        Collections.reverse(entitiesToRemove);
        for (Entity e : entitiesToRemove) {
            entityTableSql.append("DROP TABLE ")
                    .append(PostgresUtils.entityTable(networkId, e.getConceptAlias()))
                    .append(";");
        }

        jdbcTemplate.execute(entityTableSql.toString(), PreparedStatement::execute);
    }

    @Override
    @SetFimsUser
    public void save(NetworkConfig config, int networkId) {
        Map<String, Object> sqlParams = new HashMap<>();
        sqlParams.put("networkId", networkId);
        sqlParams.put("config", JacksonUtil.toString(config));

        jdbcTemplate.update(
                sql.getProperty("updateConfig"),
                sqlParams
        );
    }

    private String createEntityTableSql(Entity e, int networkId, NetworkConfig config) {
        String createSql;

        Map<String, Object> paramMap = new HashMap<>();
        paramMap.put("table", PostgresUtils.entityTable(networkId, e.getConceptAlias()));
        paramMap.put("conceptAlias", e.getConceptAlias());
        paramMap.put("networkId", networkId);


        if (e.isChildEntity()) {
            paramMap.put("parentTable", PostgresUtils.entityTable(networkId, e.getParentEntity()));
            paramMap.put("parentColumn", e.getAttributeUri(config.entity(e.getParentEntity()).getUniqueKey()));
            createSql = StrSubstitutor.replace(sql.getProperty("createEntityTable"), paramMap);
            createSql += StrSubstitutor.replace(sql.getProperty("createChildEntityTable"), paramMap);
        } else {
            createSql = StrSubstitutor.replace(sql.getProperty("createEntityTable"), paramMap);
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
