package biocode.fims.models;

import biocode.fims.authorizers.ExpeditionVisibility;
import biocode.fims.models.dataTypes.converters.UriPersistenceConverter;
import biocode.fims.serializers.JsonViewOverride;
import biocode.fims.serializers.Views;
import com.fasterxml.jackson.annotation.*;
import org.springframework.util.Assert;

import javax.persistence.*;
import java.net.URI;
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
    private ExpeditionVisibility visibility;
    private URI identifier;
    private List<EntityIdentifier> entityIdentifiers;
    private Project project;
    private User user;

    public static class ExpeditionBuilder {

        // Required
        private String expeditionCode;
        // Optional
        private String expeditionTitle;
        private boolean isPublic = true;
        private ExpeditionVisibility visibility = ExpeditionVisibility.EXPEDITION;

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

        public ExpeditionBuilder visibility(ExpeditionVisibility visibility) {
            this.visibility = visibility;
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
        visibility = builder.visibility;
    }

    // needed for hibernate
    Expedition() {
    }

    @JsonView(Views.Summary.class)
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    public int getExpeditionId() {
        return expeditionId;
    }

    private void setExpeditionId(int id) {
        this.expeditionId = id;
    }

    @JsonView(Views.Summary.class)
    @Column(nullable = false, updatable = false, name = "expedition_code")
    public String getExpeditionCode() {
        return expeditionCode;
    }

    public void setExpeditionCode(String expeditionCode) {
        this.expeditionCode = expeditionCode;
    }

    @JsonView(Views.Summary.class)
    @Column(name = "expedition_title")
    public String getExpeditionTitle() {
        return expeditionTitle;
    }

    public void setExpeditionTitle(String expeditionTitle) {
        this.expeditionTitle = expeditionTitle;
    }

    @Column(updatable = false)
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

    @JsonView(Views.Summary.class)
    @Column(name = "visibility", nullable = false)
    @Enumerated(EnumType.STRING)
    public ExpeditionVisibility getVisibility() {
        return visibility;
    }

    public void setVisibility(ExpeditionVisibility visibility) {
        Assert.notNull(visibility);
        this.visibility = visibility;
    }

    @JsonView(Views.Summary.class)
    @Convert(converter = UriPersistenceConverter.class)
    @Column(name = "identifier")
    public URI getIdentifier() {
        return identifier;
    }

    @JsonIgnore
    public void setIdentifier(URI identifier) {
        if (this.identifier == null) {
            this.identifier = identifier;
        }
    }

    @JsonView(Views.Detailed.class)
    @OneToMany(fetch = FetchType.EAGER, cascade = CascadeType.ALL)
    @JoinColumn(name = "expedition_id", nullable = false)
    public List<EntityIdentifier> getEntityIdentifiers() {
        return entityIdentifiers;
    }

    @JsonIgnore
    public void setEntityIdentifiers(List<EntityIdentifier> entityIdentifiers) {
        if (this.entityIdentifiers == null) {
            this.entityIdentifiers = entityIdentifiers;
        }
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

    @JsonView(Views.Detailed.class)
    @JsonViewOverride(Views.Summary.class)
    @ManyToOne
    @JoinColumn(name = "project_id",
            referencedColumnName = "id",
            nullable = false, updatable = false,
            foreignKey = @ForeignKey(name = "FK_expeditions_project_id")
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
    @JoinColumn(name = "user_id",
            foreignKey = @ForeignKey(name = "FK_expeditions_user_id"),
            referencedColumnName = "id"
    )
    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }
}
