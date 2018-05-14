package biocode.fims.repositories;

import biocode.fims.api.services.AbstractRequest;
import biocode.fims.application.config.FimsProperties;
import biocode.fims.bcid.Bcid;
import biocode.fims.fimsExceptions.FimsRuntimeException;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.NotAuthorizedException;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.Form;
import javax.ws.rs.core.MediaType;

/**
 * @author rjewing
 */
public class BcidRepository {
    private final static Logger logger = LoggerFactory.getLogger(BcidRepository.class);

    private final Client client;
    private final FimsProperties props;
    private AccessToken accessToken;
    private boolean triedRefresh;

    public BcidRepository(Client client, FimsProperties props) {
        this.client = client;
        this.props = props;
    }

    public Bcid create(Bcid toMint) {
        if (!authenticated() && !authenticate()) {
            throw new FimsRuntimeException("Unable to authenticate with the bcid system", 500);
        }

        return executeRequest(new MintBcid(client, props.bcidUrl(), toMint));
    }

    public Bcid get(String identifier) {
        return new FetchBcid(client, props.bcidUrl(), identifier).execute();
    }

    private <T> T executeRequest(AbstractRequest<T> request) {
        try {
            request.addHeader("Authorization", "Bearer " + accessToken.accessToken);
            return request.execute();
        } catch (WebApplicationException e) {
            if (e instanceof NotAuthorizedException && !triedRefresh) {
                this.triedRefresh = true;
                if (authenticate()) {
                    return executeRequest(request);
                }
            }
            throw e;
        }
    }

    private boolean authenticated() {
        return this.accessToken != null;
    }

    private boolean authenticate() {
        try {
            this.accessToken = new Authenticate(client, props.bcidUrl(), props.bcidClientId(), props.bcidClientSecret())
                    .execute();
        } catch (WebApplicationException e) {
            logger.error("Bcid authentication error", e);
            return false;
        }

        this.triedRefresh = false;
        return true;
    }


    private static final class FetchBcid extends AbstractRequest<Bcid> {
        private static final String path = "/";

        public FetchBcid(Client client, String baseUrl, String identifier) {
            super("GET", Bcid.class, client, path + identifier, baseUrl);

            setAccepts(MediaType.APPLICATION_JSON);
        }
    }

    private static final class MintBcid extends AbstractRequest<Bcid> {
        private static final String path = "/";

        public MintBcid(Client client, String baseUrl, Bcid toMint) {
            super("POST", Bcid.class, client, path, baseUrl);

            this.setHttpEntity(Entity.entity(toMint, MediaType.APPLICATION_JSON));
            setAccepts(MediaType.APPLICATION_JSON);
        }
    }

    private static final class Authenticate extends AbstractRequest<AccessToken> {
        private static final String PATH = "/oAuth2/token";
        private static final String GRANT_TYPE = "client_credentials";

        public Authenticate(Client client, String baseUrl, String id, String secret) {
            super("POST", AccessToken.class, client, PATH, baseUrl);


            Form form = new Form("grant_type", GRANT_TYPE)
                    .param("client_id", id)
                    .param("client_secret", secret);

            this.setHttpEntity(Entity.entity(form, MediaType.APPLICATION_FORM_URLENCODED_TYPE));
            setAccepts(MediaType.APPLICATION_JSON);
        }

    }

    private static class AccessToken {
        @JsonProperty("token_type")
        public String tokenType;
        @JsonProperty("expires_in")
        public int expiresIn;
        @JsonProperty("access_token")
        public String accessToken;
    }
}
