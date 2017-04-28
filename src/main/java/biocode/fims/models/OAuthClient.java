package biocode.fims.models;

import biocode.fims.utils.StringGenerator;

import javax.persistence.*;
import java.io.Serializable;

/**
 * OAuthClient entity object
 */
@Entity
@Table(name = "oAuthClients")
public class OAuthClient implements Serializable {
    private String clientId;
    private String clientSecret;
    private String callback;

    public static class OAuthClientBuilder {
        private final String clientId = StringGenerator.generateString(20);
        private final String clientSecret = StringGenerator.generateString(75);
        private final String callback;

        public OAuthClientBuilder(String callback) {
            this.callback = callback;
        }

        public OAuthClient build() {
            return new OAuthClient(this);
        }

    }

    private OAuthClient(OAuthClientBuilder builder) {
        this.clientId = builder.clientId;
        this.clientSecret = builder.clientSecret;
        this.callback = builder.callback;
    }

    OAuthClient() {
    }

    @Id
    @Column(nullable = false, updatable = false, unique = true)
    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public String getClientSecret() {
        return clientSecret;
    }

    public void setClientSecret(String clientSecret) {
        this.clientSecret = clientSecret;
    }

    public String getCallback() {
        return callback;
    }

    public void setCallback(String callback) {
        this.callback = callback;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof OAuthClient)) return false;

        OAuthClient that = (OAuthClient) o;

        return getClientId().equals(that.getClientId());
    }

    @Override
    public int hashCode() {
        return getClientId().hashCode();
    }
}
