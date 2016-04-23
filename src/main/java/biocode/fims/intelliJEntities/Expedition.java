package biocode.fims.intelliJEntities;

import org.springframework.transaction.annotation.Transactional;

import javax.persistence.*;
import java.sql.Timestamp;
import java.util.Date;
import java.util.Set;

/**
 * Expedition Entity object
 */
@Entity
@Table(name = "expeditions", schema = "biscicol")
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

    public static class ExpeditionBuilder {

        // Required
        private String expeditionCode;
        private User user;
        private Project project;
        // Optional
        private String expeditionTitle;
        private boolean isPublic = true;

        public ExpeditionBuilder(String expeditionCode, User user, Project project) {
            this.expeditionCode = expeditionCode;
            this.user = user;
            this.project = project;
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
        user = builder.user;
        project = builder.project;
        isPublic = builder.isPublic;
    }

    // needed for hibernate
    private Expedition() {}

    @Id
    @GeneratedValue(strategy=GenerationType.AUTO)
    public int getExpeditionId() {
        return expeditionId;
    }

    private void setExpeditionId(int id) {
        this.expeditionId = id;
    }

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

    private void setTs(Timestamp ts) {
        this.ts = ts;
    }

    public boolean isPublic() {
        return isPublic;
    }

    public void setPublic(boolean aPublic) {
        isPublic = aPublic;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Expedition that = (Expedition) o;

        if (getExpeditionId() != that.getExpeditionId()) return false;
        if (isPublic() != that.isPublic()) return false;
        if (getExpeditionCode() != null ? !getExpeditionCode().equals(that.getExpeditionCode()) : that.getExpeditionCode() != null)
            return false;
        if (getExpeditionTitle() != null ? !getExpeditionTitle().equals(that.getExpeditionTitle()) : that.getExpeditionTitle() != null)
            return false;
        if (getTs() != null ? !getTs().equals(that.getTs()) : that.getTs() != null) return false;
        if (getBcids() != null ? !getBcids().equals(that.getBcids()) : that.getBcids() != null) return false;
        if (getProject() != null ? !getProject().equals(that.getProject()) : that.getProject() != null) return false;
        return getUser() != null ? getUser().equals(that.getUser()) : that.getUser() == null;

    }

    @Override
    public int hashCode() {
        int result = getExpeditionId();
        result = 31 * result + (getExpeditionCode() != null ? getExpeditionCode().hashCode() : 0);
        result = 31 * result + (getExpeditionTitle() != null ? getExpeditionTitle().hashCode() : 0);
        result = 31 * result + (getTs() != null ? getTs().hashCode() : 0);
        result = 31 * result + (isPublic() ? 1 : 0);
        result = 31 * result + (getBcids() != null ? getBcids().hashCode() : 0);
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

    @ManyToOne
    @JoinColumn(name = "projectId",
            referencedColumnName = "projectId",
            foreignKey = @ForeignKey(name = "FK_expedtions_projectId")
    )
    public Project getProject() {
        return project;
    }

    private void setProject(Project project) {
        this.project = project;
    }


    @ManyToOne
    @JoinColumn(name = "userId",
            foreignKey = @ForeignKey(name = "FK_expeditions_userId"),
            referencedColumnName = "userId"
    )
    public User getUser() {
        return user;
    }

    private void setUser(User user) {
        this.user = user;
    }
}
