package biocode.fims.dao;

import biocode.fims.entities.Bcid;
import biocode.fims.entities.Expedition;
import biocode.fims.rowMapper.ExpeditionRowMapper;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.simple.SimpleJdbcInsert;

import javax.sql.DataSource;
import java.sql.Timestamp;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class ExpeditionDao {
    private NamedParameterJdbcTemplate namedParameterJdbcTemplate;

    private SimpleJdbcInsert insertExpedition;

    public ExpeditionDao(DataSource dataSource) {
        this.namedParameterJdbcTemplate = new NamedParameterJdbcTemplate(dataSource);

        this.insertExpedition = new SimpleJdbcInsert(dataSource)
                .withTableName("expeditions")
                .usingGeneratedKeyColumns("expeditionId");

    }

    public void update(Expedition expedition) {
        String updateTemplate = "UPDATE expeditions SET expeditionTitle=:expeditionTitle, " +
                "userId=:userId, ts=:ts, public=:public WHERE expeditionId=:expeditionId";

        this.namedParameterJdbcTemplate.update(
                updateTemplate,
                createExpeditionParameterSource(expedition));
    }

    public void create(Expedition expedition) {

        expedition.setExpeditionId(
                this.insertExpedition.executeAndReturnKey(
                    createExpeditionParameterSource(expedition)
                ).intValue()
        );
    }

    public Expedition findExpedition(MapSqlParameterSource params) throws EmptyResultDataAccessException {
        StringBuilder selectStringBuilder = new StringBuilder(
                "SELECT expeditionId, expeditionCode, expeditionTitle, userId, ts, projectId, public " +
                "FROM expeditions WHERE ");

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
                new ExpeditionRowMapper()
        );
    }

    /**
     * Fetch the Expedition associated with the bcidId provided {@link MapSqlParameterSource} params.
     * @param params the query params used to find the {@link Expedition}'s. expects bcidId to be in the params map
     * @return
     */
    public Expedition findExpeditionByBcid(MapSqlParameterSource params) throws EmptyResultDataAccessException {
         StringBuilder selectStringBuilder = new StringBuilder(
                "SELECT expeditions.expeditionId, expeditionCode, expeditionTitle, expeditions.userId, ts, projectId, " +
                        "public FROM expeditions, expeditionBcids WHERE expeditions.expeditionId=expeditionBcids.expeditionId" +
                        " and bcidId=:bcidId"
         );

        int cnt = 0;
        for (String key: params.getValues().keySet()) {
            if (!key.equals("bcidId")) {
                if (cnt > 0)
                    selectStringBuilder.append(" and ");

                selectStringBuilder.append(key);
                selectStringBuilder.append("=:");
                selectStringBuilder.append(key);

                cnt++;
            }
        }

        return this.namedParameterJdbcTemplate.queryForObject(
                selectStringBuilder.toString(),
                params,
                new ExpeditionRowMapper()
        );

    }

    public void attachBcid(Bcid bcid, Expedition expedition) {
        Map<String, Integer> params = new HashMap<>();
        params.put("bcidId", bcid.getBcidId());
        params.put("expeditionId", expedition.getExpeditionId());

        this.namedParameterJdbcTemplate.update(
                "INSERT INTO expeditionBcids (bcidId, expeditionId) VALUES (:bcidId, :expeditionId)",
                params
        );
    }

    /**
     * Creates a {@link MapSqlParameterSource} based on data values from the supplied {@link Expedition} instance.
     */
    private MapSqlParameterSource createExpeditionParameterSource(Expedition expedition) {
        return new MapSqlParameterSource()
                .addValue("expeditionId", expedition.getExpeditionId())
                .addValue("expeditionCode", expedition.getExpeditionCode())
                .addValue("expeditionTitle", expedition.getExpeditionTitle())
                .addValue("userId", expedition.getUserId())
                .addValue("ts", new Timestamp(new Date().getTime()))
                .addValue("projectId", expedition.getProjectId())
                .addValue("public", expedition.isPublic());
    }
}
