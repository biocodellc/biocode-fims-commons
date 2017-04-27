package biocode.fims.entities;

import javax.persistence.*;
import java.util.Date;

/**
 * oAutheNonce entity object
 */
@Entity
@Table(name = "oAuthNonces")
public class OAuthNonce extends BaseModel {

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

    OAuthNonce() {};

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

    @Column(nullable = false, updatable = false)
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
    @JoinColumn(name = "userId",
            foreignKey = @ForeignKey(name = "FK_expeditions_userId"),
            referencedColumnName = "id"
    )
    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "clientId",
            nullable = false, updatable = false,
            foreignKey = @ForeignKey(name = "FK_oAuthNonces_clientId"),
            referencedColumnName = "clientId")
    public OAuthClient getoAuthClient() {
        return oAuthClient;
    }

    public void setoAuthClient(OAuthClient oAuthClient) {
        this.oAuthClient = oAuthClient;
    }
}
