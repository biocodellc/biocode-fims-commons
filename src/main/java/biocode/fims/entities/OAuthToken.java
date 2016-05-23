package biocode.fims.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import javax.persistence.*;
import java.util.Date;

/**
 * OAuthToken entity object
 */
@Entity
@Table(name = "oAuthTokens")
public class OAuthToken {
    private static String TOKEN_TYPE = "bearer";

    private static int EXPIRES_IN = 3600;
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

    OAuthToken() {};

    @JsonIgnore
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public int getoAuthTokenId() {
        return oAuthTokenId;
    }

    public void setoAuthTokenId(int oAuthTokenId) {
        this.oAuthTokenId = oAuthTokenId;
    }

    @JsonProperty(value = "access_token")
    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    @JsonProperty(value = "refresh_token")
    public String getRefreshToken() {
        return refreshToken;
    }

    public void setRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
    }

    @JsonIgnore
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

    @JsonIgnore
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

    @JsonIgnore
    @ManyToOne
    @JoinColumn(name = "clientId",
            foreignKey = @ForeignKey(name = "FK_oAuthTokens_userId"),
            referencedColumnName = "clientId")
    public OAuthClient getoAuthClient() {
        return oAuthClient;
    }

    public void setoAuthClient(OAuthClient oAuthClient) {
        this.oAuthClient = oAuthClient;
    }

    @JsonProperty("token_type")
    @Transient
    public static String getTokenType() {
        return TOKEN_TYPE;
    }

    @JsonProperty("expires_in")
    @Transient
    public static int getExpiresIn() {
        return EXPIRES_IN;
    }

    @Transient
    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }
}
