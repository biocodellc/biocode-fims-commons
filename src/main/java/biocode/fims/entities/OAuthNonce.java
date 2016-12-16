package biocode.fims.entities;

import javax.persistence.*;
import java.util.Date;

/**
 * oAutheNonce entity object
 */
@Entity
@Table(name = "oAuthNonces")
public class OAuthNonce {
    private int oAuthNonceId;

    private String code;
    private Date ts;
    private String redirectUri;
    private User user;
    private OAuthClient oAuthClient;

    public OAuthNonce(String code, String redirectUri, User user, OAuthClient oAuthClient) {
        this.code = code;
        this.redirectUri = redirectUri;
        this.user = user;
        this.oAuthClient = oAuthClient;
    }

    OAuthNonce() {};

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public int getoAuthNonceId() {
        return oAuthNonceId;
    }

    public void setoAuthNonceId(int oAuthNonceId) {
        this.oAuthNonceId = oAuthNonceId;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    @Temporal(TemporalType.TIMESTAMP)
    public Date getTs() {
        return ts;
    }

    public void setTs(Date ts) {
        this.ts = ts;
    }

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

        if (oAuthNonceId != that.oAuthNonceId) return false;
        if (code != null ? !code.equals(that.code) : that.code != null) return false;
        if (ts != null ? !ts.equals(that.ts) : that.ts != null) return false;
        if (redirectUri != null ? !redirectUri.equals(that.redirectUri) : that.redirectUri != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = oAuthNonceId;
        result = 31 * result + (code != null ? code.hashCode() : 0);
        result = 31 * result + (ts != null ? ts.hashCode() : 0);
        result = 31 * result + (redirectUri != null ? redirectUri.hashCode() : 0);
        return result;
    }

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "userId",
            foreignKey = @ForeignKey(name = "FK_expeditions_userId"),
            referencedColumnName = "userId"
    )
    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "clientId",
            foreignKey = @ForeignKey(name = "FK_oAuthNonces_clientId"),
            referencedColumnName = "clientId")
    public OAuthClient getoAuthClient() {
        return oAuthClient;
    }

    public void setoAuthClient(OAuthClient oAuthClient) {
        this.oAuthClient = oAuthClient;
    }
}
