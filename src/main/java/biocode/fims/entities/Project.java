package biocode.fims.entities;

import biocode.fims.serializers.JsonViewOverride;
import biocode.fims.serializers.Views;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonView;

import javax.persistence.*;
import java.util.Date;
import java.util.List;
import java.util.Set;

/**
 * Project Entity object
 */
@Entity
@Table(name = "projects")
@NamedEntityGraphs({
        @NamedEntityGraph(name = "Project.withMembers",
                attributeNodes = @NamedAttributeNode("projectMembers")),
        @NamedEntityGraph(name = "Project.withExpeditions",
                attributeNodes = @NamedAttributeNode("expeditions")),
        @NamedEntityGraph(name = "Project.withExpeditionsAndMembers",
                attributeNodes = {@NamedAttributeNode("projectMembers"), @NamedAttributeNode("expeditions")}
        )
})
public class Project {

    private int projectId;
    private String projectCode;
    private String projectTitle;
    private Date created;
    private Date modified;
    private String validationXml;
    private boolean isPublic;
    private String projectUrl;
    private List<Expedition> expeditions;
    private User user;
    private List<User> projectMembers;
    private Set<TemplateConfig> templateConfigs;

    public static class ProjectBuilder {

        // Required
        private String projectCode;
        private String projectTitle;
        private String validationXml;
        private String projectUrl;

        // Optional
        private boolean isPublic = true;

        public ProjectBuilder(String projectCode, String projectTitle, String validationXml, String projectUrl) {
            this.projectCode = projectCode;
            this.projectTitle = projectTitle;
            this.validationXml = validationXml;
            this.projectUrl = projectUrl;
        }

        public ProjectBuilder isPublic(boolean isPublic) {
            this.isPublic = isPublic;
            return this;
        }

        public Project build() {
            return new Project(this);
        }

    }

    private Project(ProjectBuilder builder) {
        projectCode = builder.projectCode;
        projectTitle = builder.projectTitle;
        validationXml = builder.validationXml;
        projectUrl = builder.projectUrl;
        isPublic = builder.isPublic;
    }

    // needed for hibernate
    Project() {
    }

    @JsonView(Views.Summary.class)
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public int getProjectId() {
        return projectId;
    }

    public void setProjectId(int id) {
        this.projectId = id;
    }

    @JsonView(Views.Summary.class)
    public String getProjectCode() {
        return projectCode;
    }

    public void setProjectCode(String projectCode) {
        this.projectCode = projectCode;
    }

    @JsonView(Views.Summary.class)
    public String getProjectTitle() {
        return projectTitle;
    }

    public void setProjectTitle(String projectTitle) {
        this.projectTitle = projectTitle;
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

    public void setModified(Date modified) {
        this.modified = modified;
    }

    @JsonView(Views.Detailed.class)
    @Column(nullable = false)
    public String getValidationXml() {
        return validationXml;
    }

    public void setValidationXml(String validationXml) {
        this.validationXml = validationXml;
    }

    @JsonIgnore
    @Column(nullable = false)
    public String getProjectUrl() {
        return projectUrl;
    }

    public void setProjectUrl(String projectUrl) {
        this.projectUrl = projectUrl;
    }

    @JsonView(Views.Detailed.class)
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
    @JoinColumn(name = "userId",
            referencedColumnName = "userId",
            foreignKey = @ForeignKey(name = "FK_projects_userId"),
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
    public Set<TemplateConfig> getTemplateConfigs() {
        return templateConfigs;
    }

    public void setTemplateConfigs(Set<TemplateConfig> templateConfigs) {
        this.templateConfigs = templateConfigs;
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
