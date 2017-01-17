package biocode.fims.entities;

import biocode.fims.serializers.JsonViewOverride;
import biocode.fims.serializers.Views;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
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
    private String projectAbstract;
    private Date ts;
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
        private String projectAbstract;
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

        public ProjectBuilder projectAbstract(String projectAbstract) {
            this.projectAbstract = projectAbstract;
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
        projectAbstract = builder.projectAbstract;
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
    @Column(name = "abstract", columnDefinition = "text null")
    public String getProjectAbstract() {
        return projectAbstract;
    }

    public void setProjectAbstract(String projectAbstract) {
        this.projectAbstract = projectAbstract;
    }

    @JsonView(Views.Detailed.class)
    @Temporal(TemporalType.TIMESTAMP)
    public Date getTs() {
        return ts;
    }

    public void setTs(Date ts) {
        this.ts = ts;
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

        if (getProjectId() != project.getProjectId()) return false;
        if (isPublic() != project.isPublic()) return false;
        if (!getProjectCode().equals(project.getProjectCode())) return false;
        if (getProjectTitle() != null ? !getProjectTitle().equals(project.getProjectTitle()) : project.getProjectTitle() != null)
            return false;
        if (getProjectAbstract() != null ? !getProjectAbstract().equals(project.getProjectAbstract()) : project.getProjectAbstract() != null)
            return false;
        if (!getTs().equals(project.getTs())) return false;
        if (!getValidationXml().equals(project.getValidationXml())) return false;
        if (!getProjectUrl().equals(project.getProjectUrl())) return false;
        return getUser().equals(project.getUser());
    }

    @Override
    public int hashCode() {
        int result = getProjectId();
        result = 31 * result + getProjectCode().hashCode();
        result = 31 * result + (getProjectTitle() != null ? getProjectTitle().hashCode() : 0);
        result = 31 * result + (getProjectAbstract() != null ? getProjectAbstract().hashCode() : 0);
        result = 31 * result + getTs().hashCode();
        result = 31 * result + getValidationXml().hashCode();
        result = 31 * result + (isPublic() ? 1 : 0);
        result = 31 * result + getProjectUrl().hashCode();
        result = 31 * result + getUser().hashCode();
        return result;
    }

    @Override
    public String toString() {
        return "Project{" +
                "projectId=" + projectId +
                ", projectCode='" + projectCode + '\'' +
                ", projectTitle='" + projectTitle + '\'' +
                ", projectAbstract='" + projectAbstract + '\'' +
                ", ts=" + ts +
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
