package biocode.fims.dao;

import biocode.fims.bcid.BcidEncoder;
import biocode.fims.entities.Bcid;
import biocode.fims.rowMapper.BcidRowMapper;
import biocode.fims.fimsExceptions.ServerErrorException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.simple.SimpleJdbcInsert;

import javax.sql.DataSource;
import java.math.BigInteger;
import java.net.URI;
import java.net.URISyntaxException;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 *
 */
public class BcidDao {

    private String scheme = "ark:";

    private BcidEncoder bcidEncoder = new BcidEncoder();
    private NamedParameterJdbcTemplate namedParameterJdbcTemplate;

    private SimpleJdbcInsert insertBcid;

    public BcidDao(DataSource dataSource) {
        this.namedParameterJdbcTemplate = new NamedParameterJdbcTemplate(dataSource);

        this.insertBcid = new SimpleJdbcInsert(dataSource)
                .withTableName("bcids")
                .usingGeneratedKeyColumns("bcidId");

    }

    public void update(Bcid bcid) {
        String updateTemplate = "UPDATE bcids SET ezidMade=:ezidMade, ezidRequest=:ezidRequest, " +
                "identifier=:identifier, userId=:userId, " +
                "doi=:doi, title=:title, webAddress=:webAddress, resourceType=:resourceType, " +
                "ts=:ts, graph=:graph, finalCopy=:finalCopy WHERE bcidId=:bcidId";

        this.namedParameterJdbcTemplate.update(
                updateTemplate,
                createBcidParameterSource(bcid));
    }

    public void create(Bcid bcid, int naan) {

        int bcidId = this.insertBcid.executeAndReturnKey(
                createBcidParameterSource(bcid)).intValue();

        bcid.setBcidId(bcidId);

        try {
            // generate the identifier
            bcid.setIdentifier(generateBcidIdentifier(bcidId, naan));

        } catch (URISyntaxException e) {
            throw new ServerErrorException("Server Error", String.format(
                    "SQLException while creating a bcid for user: %d, bcidId: %d", bcid.getUserId(), bcid.getBcidId()),
                    e);
        }

        update(bcid);
    }

    public Bcid findBcid(MapSqlParameterSource params) {
        StringBuilder selectStringBuilder = new StringBuilder(
                "SELECT bcidId, ezidMade, ezidRequest, internalId, identifier, userId, doi, title," +
                        "webAddress, resourceType, ts, graph, finalCopy FROM bcids WHERE ");

        int cnt = 0;
        for (String key: params.getValues().keySet()) {
            if (cnt > 0)
                selectStringBuilder.append(" and ");

            // add BINARY to make identifier case sensitive
            if (key.equals("identifier"))
                selectStringBuilder.append("BINARY ");

            selectStringBuilder.append(key);
            selectStringBuilder.append("=:");
            selectStringBuilder.append(key);

            cnt++;
        }

        return this.namedParameterJdbcTemplate.queryForObject(
                selectStringBuilder.toString(),
                params,
                new BcidRowMapper()
        );
    }

    /**
     * Fetch the Bcids associated with the expeditionId provided {@link MapSqlParameterSource} params.
     * @param params the query params used to find the {@link Bcid}'s. expects expeditionId to be in the params map
     * @return
     */
    public Collection<Bcid> findBcidsAssociatedWithExpedition(MapSqlParameterSource params) {
        StringBuilder selectStringBuilder = new StringBuilder(
                "SELECT bcids.bcidId, ezidMade, ezidRequest, internalId, identifier, userId, doi, title," +
                        "webAddress, resourceType, ts, graph, finalCopy FROM bcids, expeditionBcids WHERE " +
                        "bcids.bcidId = expeditionBcids.bcidId and ");

        int cnt = 0;
        for (String key: params.getValues().keySet()) {
            if (cnt > 0)
                selectStringBuilder.append(" and ");

            selectStringBuilder.append(key);
            if (params.getValue(key) instanceof List) {
                selectStringBuilder.append(" IN (:");
                selectStringBuilder.append(key);
                selectStringBuilder.append(")");
            } else {
                selectStringBuilder.append("=:");
                selectStringBuilder.append(key);
            }

            cnt++;
        }

        return this.namedParameterJdbcTemplate.query(
                selectStringBuilder.toString(),
                params,
                new BcidRowMapper()
        );
    }

    private URI generateBcidIdentifier(int bcidId, int naan) throws URISyntaxException {
        String bow = scheme + "/" + naan+ "/";

        // Create the shoulder Bcid (String Bcid Bcid)
        String shoulder = bcidEncoder.encode(new BigInteger(String.valueOf(bcidId)));

        // Create the identifier
        return new URI(bow + shoulder);
    }

    /**
     * Creates a {@link MapSqlParameterSource} based on data values from the supplied {@link Bcid} instance.
     */
    private MapSqlParameterSource createBcidParameterSource(Bcid bcid) {
        return new MapSqlParameterSource()
                .addValue("bcidId", bcid.getBcidId())
                .addValue("ezidMade", bcid.isEzidMade())
                .addValue("ezidRequest", bcid.isEzidRequest())
                .addValue("identifier", bcid.getIdentifier(), Types.VARCHAR)
                .addValue("userId", bcid.getUserId())
                .addValue("doi", bcid.getDoi())
                .addValue("title", bcid.getTitle())
                .addValue("webAddress", bcid.getWebAddress())
                .addValue("resourceType", bcid.getResourceType())
                .addValue("ts", new Timestamp(new Date().getTime()))
                .addValue("graph", bcid.getGraph())
                .addValue("finalCopy", bcid.isFinalCopy());
    }


}
