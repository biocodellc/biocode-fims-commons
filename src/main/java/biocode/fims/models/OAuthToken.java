package biocode.fims.models;

import biocode.fims.serializers.JsonViewOverride;
import biocode.fims.serializers.OAuthTokenSerializer;
import biocode.fims.serializers.Views;
import biocode.fims.service.OAuthProviderService;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonView;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import javax.persistence.*;
import java.util.Date;

/**
 * OAuthToken entity object
 */
@JsonSerialize(using = OAuthTokenSerializer.class)
@Entity
@Table(name = "oauth_tokens")
public class OAuthToken {
    public final static String TOKEN_TYPE = "bearer";
    public final static long EXPIRES_IN = OAuthProviderService.ACCESS_TOKEN_EXPIRATION_INTEVAL;

    private int oAuthTokenId;
    private String token;
    private String refreshToken;
    private String state;
    private Date created;
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

    @JsonIgnore
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    public int getoAuthTokenId() {
        return oAuthTokenId;
    }

    public void setoAuthTokenId(int oAuthTokenId) {
        this.oAuthTokenId = oAuthTokenId;
    }

    @JsonView(Views.Summary.class)
    @Column(nullable = false, updatable = false)
    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    @JsonView(Views.Summary.class)
    @Column(nullable = false, updatable = false, name = "refresh_token")
    public String getRefreshToken() {
        return refreshToken;
    }

    public void setRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
    }

    @JsonView(Views.Summary.class)
    @Temporal(TemporalType.TIMESTAMP)
    public Date getCreated() {
        return created;
    }

    public void setCreated(Date created) {
        this.created = created;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        OAuthToken that = (OAuthToken) o;

        if (!token.equals(that.token)) return false;
        if (!refreshToken.equals(that.refreshToken)) return false;
        return oAuthClient.equals(that.oAuthClient);
    }

    @Override
    public int hashCode() {
        int result = token.hashCode();
        result = 31 * result + refreshToken.hashCode();
        result = 31 * result + oAuthClient.hashCode();
        return result;
    }

    @JsonView(Views.Detailed.class)
    @JsonViewOverride(Views.Summary.class)
    @ManyToOne
    @JoinColumn(name = "user_id",
            foreignKey = @ForeignKey(name = "FK_oauth_tokens_user_id"),
            referencedColumnName = "id"
    )
    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    @JsonView(Views.Detailed.class)
    @JsonViewOverride(Views.Summary.class)
    @ManyToOne
    @JoinColumn(name = "client_id",
            nullable = false, updatable = false,
            foreignKey = @ForeignKey(name = "FK_oauth_tokens_client_id"),
            referencedColumnName = "id")
    public OAuthClient getoAuthClient() {
        return oAuthClient;
    }

    public void setoAuthClient(OAuthClient oAuthClient) {
        this.oAuthClient = oAuthClient;
    }

    @JsonView(Views.Summary.class)
    @Transient
    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }
}
