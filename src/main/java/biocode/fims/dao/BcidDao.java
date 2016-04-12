package biocode.fims.dao;

import biocode.fims.bcid.BcidEncoder;
import biocode.fims.entities.Bcid;
import biocode.fims.fimsExceptions.ServerErrorException;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.simple.SimpleJdbcInsert;

import javax.sql.DataSource;
import java.math.BigInteger;
import java.net.URI;
import java.net.URISyntaxException;

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
                .withTableName("bcid")
                .usingGeneratedKeyColumns("bcidId");

    }

    public void update(Bcid bcid) {
        String updateTemplate = "UPDATE bcid SET ezidMade=:ezidMade, ezidRequest=:ezidRequest, " +
                "suffixPassThrough=:suffixPassThrough, internalId=:internalId, identifier=:identifier, " +
                "userId=:userId, doi=:doi, title=:title, webAddress=:webAddress, resourceType=:resourceType, " +
                "ts=:ts, graph=:graph, finalCopy=:finalCopy";

        this.namedParameterJdbcTemplate.update(
                updateTemplate,
                createBcidParameterSource(bcid));
    }

    public void create(Bcid bcid, int naan) {

        int bcidId = (int) this.insertBcid.executeAndReturnKey(
                createBcidParameterSource(bcid));

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
                .addValue("suffixPassThrough", bcid.isSuffixPassThrough())
                .addValue("internalId", bcid.getInternalId())
                .addValue("identifier", bcid.getIdentifier())
                .addValue("userId", bcid.getUserId())
                .addValue("doi", bcid.getDoi())
                .addValue("title", bcid.getTitle())
                .addValue("webAddress", bcid.getWebAddress())
                .addValue("resourceType", bcid.getResourceType())
                .addValue("ts", "now()")
                .addValue("graph", bcid.getGraph())
                .addValue("finalCopy", bcid.isFinalCopy());
    }


}
