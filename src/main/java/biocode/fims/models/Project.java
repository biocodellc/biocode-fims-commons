package biocode.fims.models;

import biocode.fims.models.dataTypes.JsonBinaryType;
import biocode.fims.projectConfig.ProjectConfig;
import biocode.fims.serializers.JsonViewOverride;
import biocode.fims.serializers.Views;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonView;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;

import javax.persistence.*;
import java.util.Date;
import java.util.List;
import java.util.Set;

/**
 * Project Entity object
 */
@JsonIgnoreProperties({ "config" })
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
    private String validationXml;
    private String description;
    private ProjectConfig projectConfig;
    private boolean isPublic;
    private String projectUrl;
    private List<Expedition> expeditions;
    private User user;
    private List<User> projectMembers;
    private Set<ProjectTemplate> templates;

    public static class ProjectBuilder {

        // Required
        private String projectCode;
        private String projectTitle;
        private ProjectConfig projectConfig;
        private String projectUrl;

        // Optional
        private boolean isPublic = true;
        private String description;

        public ProjectBuilder(String projectCode, String projectTitle, ProjectConfig projectConfig, String projectUrl) {
            this.projectCode = projectCode;
            this.projectTitle = projectTitle;
            this.projectConfig = projectConfig;
            this.projectUrl = projectUrl;
        }

        public ProjectBuilder isPublic(boolean isPublic) {
            this.isPublic = isPublic;
            return this;
        }

        public ProjectBuilder description(String description) {
            this.description = description;
            return this;
        }

        public Project build() {
            return new Project(this);
        }

    }

    private Project(ProjectBuilder builder) {
        projectCode = builder.projectCode;
        projectTitle = builder.projectTitle;
        projectConfig = builder.projectConfig;
        projectUrl = builder.projectUrl;
        isPublic = builder.isPublic;
        description = builder.description;
    }

    // needed for hibernate
    Project() {
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

    @JsonView(Views.Detailed.class)
    @Column(nullable = false, name = "validation_xml")
    public String getValidationXml() {
        return validationXml;
    }

    public void setValidationXml(String validationXml) {
        this.validationXml = validationXml;
    }

    @JsonIgnore
    @Type(type = "jsonb")
    @Column(columnDefinition = "jsonb", name = "config")
    public ProjectConfig getProjectConfig() {
        return projectConfig;
    }

    public void setProjectConfig(ProjectConfig projectConfig) {
        this.projectConfig = projectConfig;
    }

    @JsonIgnore
    @Column(nullable = false, name = "project_url")
    public String getProjectUrl() {
        return projectUrl;
    }

    public void setProjectUrl(String projectUrl) {
        this.projectUrl = projectUrl;
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
        return 31;
    }

    @Override
    public String toString() {
        return "Project{" +
                "projectId=" + projectId +
                ", projectCode='" + projectCode + '\'' +
                ", projectTitle='" + projectTitle + '\'' +
                ", created=" + created +
                ", modified=" + modified +
                ", validationXml='" + validationXml + '\'' +
                ", isPublic=" + isPublic +
                ", projectUrl='" + projectUrl + '\'' +
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
    @JsonViewOverride(Views.Summary.class)
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
    public Set<ProjectTemplate> getTemplates() {
        return templates;
    }

    public void setTemplates(Set<ProjectTemplate> templates) {
        this.templates = templates;
    }

    // TODO move this to projectService?
    public Expedition getExpedition(String expeditionCode) {
        for (Expedition expedition : getExpeditions()) {
            if (expedition.getExpeditionCode().equals(expeditionCode))
                return expedition;
        }
        return null;
    }
}
