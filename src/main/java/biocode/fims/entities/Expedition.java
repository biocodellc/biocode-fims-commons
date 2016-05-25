package biocode.fims.entities;

import com.fasterxml.jackson.annotation.*;

import javax.persistence.*;
import java.sql.Timestamp;
import java.util.Date;
import java.util.Set;

/**
 * Expedition Entity object
 */
@Entity
@Table(name = "expeditions")
public class Expedition {
    public static final String EXPEDITION_RESOURCE_TYPE = "http://purl.org/dc/dcmitype/Collection";

    private int expeditionId;
    private String expeditionCode;
    private String expeditionTitle;
    private Date ts;
    private boolean isPublic;
    private Set<Bcid> bcids;
    private Project project;
    private User user;
    private Bcid expeditionBcid;

    public static class ExpeditionBuilder {

        // Required
        private String expeditionCode;
        // Optional
        private String expeditionTitle;
        private boolean isPublic = true;

        public ExpeditionBuilder(String expeditionCode) {
            this.expeditionCode = expeditionCode;
        }

        public ExpeditionBuilder expeditionTitle(String expeditionTitle) {
            this.expeditionTitle = expeditionTitle;
            return this;
        }

        public ExpeditionBuilder isPublic(boolean isPublic) {
            this.isPublic = isPublic;
            return this;
        }

        public Expedition build() {
            if (expeditionTitle == null)
                expeditionTitle = expeditionCode + " dataset";
            return new Expedition(this);
        }

    }
    private Expedition(ExpeditionBuilder builder) {
        expeditionCode = builder.expeditionCode;
        expeditionTitle = builder.expeditionTitle;
        isPublic = builder.isPublic;
    }

    // needed for hibernate
    Expedition() {}

    @Id
    @GeneratedValue(strategy=GenerationType.IDENTITY)
    public int getExpeditionId() {
        return expeditionId;
    }

    private void setExpeditionId(int id) {
        this.expeditionId = id;
    }

    @Column(nullable = false)
    public String getExpeditionCode() {
        return expeditionCode;
    }

    public void setExpeditionCode(String expeditionCode) {
        this.expeditionCode = expeditionCode;
    }

    public String getExpeditionTitle() {
        return expeditionTitle;
    }

    public void setExpeditionTitle(String expeditionTitle) {
        this.expeditionTitle = expeditionTitle;
    }

    @Temporal(TemporalType.TIMESTAMP)
    public Date getTs() {
        return ts;
    }

    private void setTs(Date ts) {
        this.ts = ts;
    }

    @Column(name = "public",
            nullable = false)
    public boolean isPublic() {
        return isPublic;
    }

    public void setPublic(boolean aPublic) {
        isPublic = aPublic;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Expedition)) return false;

        Expedition that = (Expedition) o;

        if (this.getExpeditionId() != 0 && that.getExpeditionId() != 0)
            return this.getExpeditionId() == that.getExpeditionId();

        if (isPublic() != that.isPublic()) return false;
        if (!getExpeditionCode().equals(that.getExpeditionCode())) return false;
        if (getExpeditionTitle() != null ? !getExpeditionTitle().equals(that.getExpeditionTitle()) : that.getExpeditionTitle() != null)
            return false;
        if (getProject() != null ? !getProject().equals(that.getProject()) : that.getProject() != null) return false;
        return getUser() != null ? getUser().equals(that.getUser()) : that.getUser() == null;

    }

    @Override
    public int hashCode() {
        int result = getExpeditionCode().hashCode();
        result = 31 * result + (getExpeditionTitle() != null ? getExpeditionTitle().hashCode() : 0);
        result = 31 * result + (isPublic() ? 1 : 0);
        result = 31 * result + (getProject() != null ? getProject().hashCode() : 0);
        result = 31 * result + (getUser() != null ? getUser().hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "Expedition{" +
                "expeditionId=" + expeditionId +
                ", expeditionCode='" + expeditionCode + '\'' +
                ", expeditionTitle='" + expeditionTitle + '\'' +
                ", ts=" + ts +
                ", isPublic=" + isPublic +
                ", project=" + project +
                ", user=" + user +
                ", public=" + isPublic() +
                '}';
    }

    @JsonIgnore
    @OneToMany(targetEntity = Bcid.class,
            mappedBy = "expedition",
            fetch = FetchType.LAZY
    )
    public Set<Bcid> getBcids() {
        return bcids;
    }

    private void setBcids(Set<Bcid> bcids) {
        this.bcids = bcids;
    }

    @JsonProperty(value = "projectId")
    @JsonIdentityInfo(generator = ObjectIdGenerators.PropertyGenerator.class, property = "projectId")
    @JsonIdentityReference(alwaysAsId = true)
    @ManyToOne
    @JoinColumn(name = "projectId",
            referencedColumnName = "projectId",
            foreignKey = @ForeignKey(name = "FK_expedtions_projectId")
    )
    public Project getProject() {
        return project;
    }

    public void setProject(Project project) {
        this.project = project;
    }


    @JsonProperty(value = "userId")
    @JsonIdentityInfo(generator = ObjectIdGenerators.PropertyGenerator.class, property = "userId")
    @JsonIdentityReference(alwaysAsId = true)
    @ManyToOne
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

    @Transient
    @JsonIdentityInfo(generator = ObjectIdGenerators.PropertyGenerator.class, property="identifier")
    @JsonIdentityReference(alwaysAsId = true)
    public Bcid getExpeditionBcid() {
        return expeditionBcid;
    }

    public void setExpeditionBcid(Bcid bcid) {
        expeditionBcid = bcid;
    }
}
