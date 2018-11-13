package biocode.fims.models;

import biocode.fims.config.project.ProjectConfig;
import biocode.fims.config.project.models.PersistedProjectConfig;
import biocode.fims.models.dataTypes.JsonBinaryType;
import biocode.fims.serializers.JsonViewOverride;
import biocode.fims.serializers.Views;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonView;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;

import javax.persistence.*;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Project Entity object
 */
@JsonIgnoreProperties(value = {"config"}, ignoreUnknown = true)
@TypeDef(name = "jsonb", typeClass = JsonBinaryType.class)
@Entity
@Table(name = "projects")
@NamedEntityGraphs({
        @NamedEntityGraph(name = "Project.withMembers",
                attributeNodes = @NamedAttributeNode("projectMembers")),
        @NamedEntityGraph(name = "Project.withExpeditions",
                attributeNodes = @NamedAttributeNode("expeditions")),
        @NamedEntityGraph(name = "Project.withExpeditionsAndMembers",
                attributeNodes = {@NamedAttributeNode("projectMembers"), @NamedAttributeNode("expeditions")}
        ),
        @NamedEntityGraph(name = "Project.withTemplates",
                attributeNodes = @NamedAttributeNode("templates"))
})
public class Project {

    private int projectId;
    private String projectCode;
    private String projectTitle;
    private Date created;
    private Date modified;
    private String description;
    private ProjectConfiguration projectConfiguration;
    private boolean isPublic;
    private List<Expedition> expeditions;
    private User user;
    private Network network;
    private List<User> projectMembers;
    private Set<WorksheetTemplate> templates;

    public static class ProjectBuilder {

        // Required
        private String description;
        private String projectTitle;
        public ProjectConfiguration projectConfiguration;

        // Optional
        private boolean isPublic = true;
        private String projectCode;

        public ProjectBuilder(String description, String projectTitle, ProjectConfiguration projectConfiguration) {
            this.description = description;
            this.projectTitle = projectTitle;
            this.projectConfiguration = projectConfiguration;
        }

        public ProjectBuilder isPublic(boolean isPublic) {
            this.isPublic = isPublic;
            return this;
        }

        public ProjectBuilder projectCode(String projectCode) {
            this.projectCode = projectCode;
            return this;
        }

        public Project build() {
            return new Project(this);
        }

    }

    private Project(ProjectBuilder builder) {
        projectCode = builder.projectCode;
        projectTitle = builder.projectTitle;
        projectConfiguration = builder.projectConfiguration;
        isPublic = builder.isPublic;
        description = builder.description;
    }

    // needed for hibernate
    protected Project() {
    }

    @JsonView(Views.Summary.class)
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    public int getProjectId() {
        return projectId;
    }

    public void setProjectId(int id) {
        this.projectId = id;
    }

    @JsonView(Views.Summary.class)
    @Column(name = "project_code")
    public String getProjectCode() {
        return projectCode;
    }

    public void setProjectCode(String projectCode) {
        this.projectCode = projectCode;
    }

    @JsonView(Views.Summary.class)
    @Column(name = "project_title")
    public String getProjectTitle() {
        return projectTitle;
    }

    public void setProjectTitle(String projectTitle) {
        this.projectTitle = projectTitle;
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

    public void setModified(Date modified) {
        this.modified = modified;
    }

    @JsonView(Views.DetailedConfig.class)
    @Transient
    public ProjectConfig getProjectConfig() {
        return projectConfiguration == null ? null : projectConfiguration.getProjectConfig();
    }

    @JsonView(Views.Detailed.class)
    @Column(name = "public", nullable = false)
    public boolean isPublic() {
        return isPublic;
    }

    public void setPublic(boolean aPublic) {
        isPublic = aPublic;
    }

    @JsonView(Views.Detailed.class)
    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Project)) return false;

        Project project = (Project) o;

        return getProjectId() != 0 && getProjectId() == project.getProjectId();
    }

    @Override
    public int hashCode() {
        return getProjectId();
    }

    @Override
    public String toString() {
        return "Project{" +
                "projectId=" + projectId +
                ", projectCode='" + projectCode + '\'' +
                ", projectTitle='" + projectTitle + '\'' +
                ", created=" + created +
                ", modified=" + modified +
                ", isPublic=" + isPublic +
                ", user=" + user +
                ", public=" + isPublic() +
                '}';
    }

    @JsonIgnore
    @OneToMany(mappedBy = "project",
            fetch = FetchType.LAZY
    )
    public List<Expedition> getExpeditions() {
        return expeditions;
    }

    public void setExpeditions(List<Expedition> expeditions) {
        this.expeditions = expeditions;
    }

    @JsonView(Views.Detailed.class)
    @ManyToOne
    @JoinColumn(name = "user_id",
            referencedColumnName = "id",
            foreignKey = @ForeignKey(name = "FK_projects_user_id"),
            nullable = false

    )
    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    /**
     * you probably want {@link #getProjectConfig()}
     *
     * @return
     */
    @JsonView(Views.Detailed.class)
    @JsonViewOverride(Views.Summary.class)
    @ManyToOne
    @JoinColumn(name = "config_id",
            referencedColumnName = "id"
    )
    public ProjectConfiguration getProjectConfiguration() {
        return projectConfiguration;
    }

    public void setProjectConfiguration(ProjectConfiguration projectConfiguration) {
        this.projectConfiguration = projectConfiguration;
    }

    @JsonView(Views.Detailed.class)
    @JsonViewOverride(Views.Summary.class)
    @ManyToOne
    @JoinColumn(name = "network_id",
            referencedColumnName = "id",
            foreignKey = @ForeignKey(name = "FK_projects_network_id"),
            nullable = false
    )
    public Network getNetwork() {
        return network;
    }

    public void setNetwork(Network network) {
        this.network = network;
    }

    @JsonIgnore
    @ManyToMany(mappedBy = "projectsMemberOf",
            fetch = FetchType.LAZY
    )
    public List<User> getProjectMembers() {
        return projectMembers;
    }

    public void setProjectMembers(List<User> projectMembers) {
        this.projectMembers = projectMembers;
    }

    @JsonIgnore
    @OneToMany(mappedBy = "project",
            fetch = FetchType.LAZY
    )
    public Set<WorksheetTemplate> getTemplates() {
        return templates;
    }

    public void setTemplates(Set<WorksheetTemplate> templates) {
        this.templates = templates;
    }
}
