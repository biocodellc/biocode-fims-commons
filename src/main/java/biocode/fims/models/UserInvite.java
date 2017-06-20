package biocode.fims.models;

import org.hibernate.annotations.Type;
import org.springframework.util.Assert;

import javax.persistence.*;
import java.util.Date;
import java.util.UUID;

/**
 * @author rjewing
 */
@Entity
@Table(name = "user_invite")
public class UserInvite {

    private User invitedBy;
    private UUID id;
    private String email;
    private Project project;
    private Date created;

    UserInvite() {}

    public UserInvite(String email, Project project, User invitedBy) {
        Assert.notNull(email);
        Assert.notNull(project);
        Assert.notNull(invitedBy);
        this.email = email;
        this.project = project;
        this.invitedBy = invitedBy;
        this.id = UUID.randomUUID();
    }

    @Id
    @Type(type = "java.util.UUID")
    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

//    @MapsId()
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "project_id",
            referencedColumnName = "id",
            nullable = false, updatable = false,
            foreignKey = @ForeignKey(name = "FK_user_invite_project_id")
    )
    public Project getProject() {
        return project;
    }

    public void setProject(Project project) {
        this.project = project;
    }

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "invited_by_id",
            referencedColumnName = "id",
            nullable = false, updatable = false,
            foreignKey = @ForeignKey(name = "FK_user_invite_invited_by_id")
    )
    public User getInvitedBy() {
        return invitedBy;
    }

    public void setInvitedBy(User invitedBy) {
        this.invitedBy = invitedBy;
    }

    @Column(updatable = false)
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
        if (!(o instanceof UserInvite)) return false;

        UserInvite that = (UserInvite) o;

        return getId() != null ? getId().equals(that.getId()) : that.getId() == null;
    }

    @Override
    public int hashCode() {
        return getId() != null ? getId().hashCode() : 0;
    }
}
