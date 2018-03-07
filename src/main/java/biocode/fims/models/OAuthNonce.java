package biocode.fims.models;

import javax.persistence.*;
import java.util.Date;

/**
 * oAutheNonce queryEntity object
 */
@Entity
@Table(name = "oauth_nonces")
public class OAuthNonce {
    private int oAuthNonceId;

    private String code;
    private Date created;
    private String redirectUri;
    private User user;
    private OAuthClient oAuthClient;

    public OAuthNonce(String code, String redirectUri, User user, OAuthClient oAuthClient) {
        this.code = code;
        this.redirectUri = redirectUri;
        this.user = user;
        this.oAuthClient = oAuthClient;
    }

    OAuthNonce() {
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    public int getoAuthNonceId() {
        return oAuthNonceId;
    }

    public void setoAuthNonceId(int oAuthNonceId) {
        this.oAuthNonceId = oAuthNonceId;
    }

    @Column(nullable = false, updatable = false)
    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    @Temporal(TemporalType.TIMESTAMP)
    public Date getCreated() {
        return created;
    }

    public void setCreated(Date created) {
        this.created = created;
    }

    @Column(nullable = false, updatable = false, name = "redirect_uri")
    public String getRedirectUri() {
        return redirectUri;
    }

    public void setRedirectUri(String redirectUri) {
        this.redirectUri = redirectUri;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        OAuthNonce that = (OAuthNonce) o;

        if (!code.equals(that.code)) return false;
        if (!redirectUri.equals(that.redirectUri)) return false;

        return oAuthClient.equals(that.oAuthClient);
    }

    @Override
    public int hashCode() {
        int result = code.hashCode();
        result = 31 * result + redirectUri.hashCode();
        result = 31 * result + oAuthClient.hashCode();
        return result;
    }

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id",
            foreignKey = @ForeignKey(name = "FK_oauth_nonces_user_id"),
            referencedColumnName = "id"
    )
    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "client_id",
            nullable = false, updatable = false,
            foreignKey = @ForeignKey(name = "FK_oauth_nonces_client_id"),
            referencedColumnName = "id")
    public OAuthClient getoAuthClient() {
        return oAuthClient;
    }

    public void setoAuthClient(OAuthClient oAuthClient) {
        this.oAuthClient = oAuthClient;
    }
}
