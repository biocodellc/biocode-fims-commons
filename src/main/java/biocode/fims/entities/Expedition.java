package biocode.fims.entities;

import biocode.fims.serializers.JsonViewOverride;
import biocode.fims.serializers.Views;
import com.fasterxml.jackson.annotation.*;

import javax.persistence.*;
import java.util.Date;
import java.util.List;
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
    private Date created;
    private Date modified;
    private boolean isPublic;
    private Set<Bcid> bcids;
    private Project project;
    private User user;
    private Bcid expeditionBcid;
    private List<Bcid> entityBcids;

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
                expeditionTitle = expeditionCode;
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

    @JsonView(Views.Summary.class)
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public int getExpeditionId() {
        return expeditionId;
    }

    private void setExpeditionId(int id) {
        this.expeditionId = id;
    }

    @JsonView(Views.Summary.class)
    @Column(nullable = false, updatable = false)
    public String getExpeditionCode() {
        return expeditionCode;
    }

    public void setExpeditionCode(String expeditionCode) {
        this.expeditionCode = expeditionCode;
    }

    @JsonView(Views.Summary.class)
    public String getExpeditionTitle() {
        return expeditionTitle;
    }

    public void setExpeditionTitle(String expeditionTitle) {
        this.expeditionTitle = expeditionTitle;
    }

    @JsonView(Views.Detailed.class)
    @Temporal(TemporalType.TIMESTAMP)
    public Date getCreated() {
        return created;
    }

    private void setCreated(Date created) {
        this.created = created;
    }

    @JsonView(Views.Detailed.class)
    @Temporal(TemporalType.TIMESTAMP)
    public Date getModified() {
        return modified;
    }

    private void setModified(Date modified) {
        this.modified = modified;
    }

    @JsonView(Views.Summary.class)
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

        if (!getExpeditionCode().equals(that.getExpeditionCode())) return false;
        return getProject().equals(that.getProject());

    }

    @Override
    public int hashCode() {
        int result = getExpeditionCode().hashCode();
        result = 31 * result + (getProject() != null ? getProject().hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "Expedition{" +
                "expeditionId=" + expeditionId +
                ", expeditionCode='" + expeditionCode + '\'' +
                ", expeditionTitle='" + expeditionTitle + '\'' +
                ", created=" + created +
                ", modified=" + modified +
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

    @JsonView(Views.Detailed.class)
    @JsonViewOverride(Views.Summary.class)
    @ManyToOne
    @JoinColumn(name = "projectId",
            referencedColumnName = "projectId",
            nullable = false, updatable = false,
            foreignKey = @ForeignKey(name = "FK_expedtions_projectId")
    )
    public Project getProject() {
        return project;
    }

    public void setProject(Project project) {
        this.project = project;
    }


    @JsonView(Views.Detailed.class)
    @JsonViewOverride(Views.Summary.class)
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

    @JsonView(Views.Detailed.class)
    @JsonViewOverride(Views.Summary.class)
    @Transient
    public Bcid getExpeditionBcid() {
        return expeditionBcid;
    }

    public void setExpeditionBcid(Bcid bcid) {
        expeditionBcid = bcid;
    }

    @JsonView(Views.Detailed.class)
    @JsonViewOverride(Views.Summary.class)
    @Transient
    public List<Bcid> getEntityBcids() {
        return entityBcids;
    }

    public void setEntityBcids(List<Bcid> entityBcids) {
        this.entityBcids = entityBcids;
    }
}