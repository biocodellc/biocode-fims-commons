package biocode.fims.entities;

import biocode.fims.serializers.OAuthTokenSerializer;
import biocode.fims.service.OAuthProviderService;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import javax.persistence.*;
import java.util.Date;

/**
 * OAuthToken entity object
 */
@JsonSerialize(using = OAuthTokenSerializer.class)
@Entity
@Table(name = "oAuthTokens")
public class OAuthToken {
    public final static String TOKEN_TYPE = "bearer";
    public final static long EXPIRES_IN = OAuthProviderService.ACCESS_TOKEN_EXPIRATION_INTEVAL;

    private int oAuthTokenId;
    private String token;
    private String refreshToken;
    private String state;
    private Date ts;
    private OAuthClient oAuthClient;

    private User user;

    public OAuthToken(String token, String refreshToken, OAuthClient oAuthClient, User user) {
        this.token = token;
        this.refreshToken = refreshToken;
        this.oAuthClient = oAuthClient;
        this.user = user;
    }

    OAuthToken() {
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public int getoAuthTokenId() {
        return oAuthTokenId;
    }

    public void setoAuthTokenId(int oAuthTokenId) {
        this.oAuthTokenId = oAuthTokenId;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getRefreshToken() {
        return refreshToken;
    }

    public void setRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
    }

    @Temporal(TemporalType.TIMESTAMP)
    public Date getTs() {
        return ts;
    }

    public void setTs(Date ts) {
        this.ts = ts;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        OAuthToken that = (OAuthToken) o;

        if (oAuthTokenId != that.oAuthTokenId) return false;
        if (token != null ? !token.equals(that.token) : that.token != null) return false;
        if (refreshToken != null ? !refreshToken.equals(that.refreshToken) : that.refreshToken != null) return false;
        if (ts != null ? !ts.equals(that.ts) : that.ts != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = oAuthTokenId;
        result = 31 * result + (token != null ? token.hashCode() : 0);
        result = 31 * result + (refreshToken != null ? refreshToken.hashCode() : 0);
        result = 31 * result + (ts != null ? ts.hashCode() : 0);
        return result;
    }

    @ManyToOne
    @JoinColumn(name = "userId",
            foreignKey = @ForeignKey(name = "FK_oAuthTokens_userId"),
            referencedColumnName = "userId"
    )
    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    @ManyToOne
    @JoinColumn(name = "clientId",
            foreignKey = @ForeignKey(name = "FK_oAuthTokens_clientId"),
            referencedColumnName = "clientId")
    public OAuthClient getoAuthClient() {
        return oAuthClient;
    }

    public void setoAuthClient(OAuthClient oAuthClient) {
        this.oAuthClient = oAuthClient;
    }

    @Transient
    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }
}
