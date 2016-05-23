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
    private int oAuthClientId;
    private String clientId;
    private String clientSecret;
    private String callback;
    private Set<OAuthNonce> oAuthNonces;
    private Set<OAuthToken> oAuthTokens;

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
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public int getoAuthClientId() {
        return oAuthClientId;
    }

    public void setoAuthClientId(int oAuthClientId) {
        this.oAuthClientId = oAuthClientId;
    }

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
        if (o == null || getClass() != o.getClass()) return false;

        OAuthClient that = (OAuthClient) o;

        if (oAuthClientId != that.oAuthClientId) return false;
        if (clientId != null ? !clientId.equals(that.clientId) : that.clientId != null) return false;
        if (clientSecret != null ? !clientSecret.equals(that.clientSecret) : that.clientSecret != null) return false;
        if (callback != null ? !callback.equals(that.callback) : that.callback != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = oAuthClientId;
        result = 31 * result + (clientId != null ? clientId.hashCode() : 0);
        result = 31 * result + (clientSecret != null ? clientSecret.hashCode() : 0);
        result = 31 * result + (callback != null ? callback.hashCode() : 0);
        return result;
    }

    @OneToMany(mappedBy = "oAuthClient",
            fetch = FetchType.LAZY
    )
    public Set<OAuthNonce> getoAuthNonces() {
        return oAuthNonces;
    }

    public void setoAuthNonces(Set<OAuthNonce> oAuthNonces) {
        this.oAuthNonces = oAuthNonces;
    }

    @OneToMany(mappedBy = "oAuthClient",
            fetch = FetchType.LAZY
    )
    public Set<OAuthToken> getoOAuthTokens() {
        return oAuthTokens;
    }

    public void setoOAuthTokens(Set<OAuthToken> oAuthTokens) {
        this.oAuthTokens = oAuthTokens;
    }
}
