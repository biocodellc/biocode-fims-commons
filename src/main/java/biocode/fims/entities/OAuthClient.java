package biocode.fims.entities;

import biocode.fims.utils.StringGenerator;

import javax.persistence.*;
import java.io.Serializable;
import java.util.Set;

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

        if (!getClientId().equals(that.getClientId())) return false;
        if (!getClientSecret().equals(that.getClientSecret())) return false;
        return getCallback().equals(that.getCallback());
    }

    @Override
    public int hashCode() {
        int result = getClientId().hashCode();
        result = 31 * result + getClientSecret().hashCode();
        result = 31 * result + getCallback().hashCode();
        return result;
    }
}
