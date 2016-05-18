package biocode.fims.entities;

import org.apache.commons.lang.builder.EqualsBuilder;

import javax.persistence.*;
import java.util.Date;
import java.util.Set;

/**
 * Project Entity object
 */
@Entity
@Table(name = "projects")
public class Project {

    private int projectId;
    private String projectCode;
    private String projectTitle;
    private String projectAbstract;
    private Date ts;
    private String validationXml;
    private boolean isPublic;
    private Set<Expedition> expeditions;
    private User user;
    private Set<User> projectMembers;
    private Set<TemplateConfig> templateConfigs;

    public static class ProjectBuilder {

        // Required
        private String projectCode;
        private String projectTitle;
        private String validationXml;

        // Optional
        private String projectAbstract;
        private boolean isPublic = true;

        public ProjectBuilder(String projectCode, String projectTitle, String validationXml) {
            this.projectCode = projectCode;
            this.projectTitle = projectTitle;
            this.validationXml = validationXml;
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
        projectAbstract = builder.projectAbstract;
        isPublic = builder.isPublic;
    }

    // needed for hibernate
    Project() {
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public int getProjectId() {
        return projectId;
    }

    public void setProjectId(int id) {
        this.projectId = id;
    }

    public String getProjectCode() {
        return projectCode;
    }

    public void setProjectCode(String projectCode) {
        this.projectCode = projectCode;
    }

    public String getProjectTitle() {
        return projectTitle;
    }

    public void setProjectTitle(String projectTitle) {
        this.projectTitle = projectTitle;
    }

    @Column(name="abstract", columnDefinition = "text null")
    public String getProjectAbstract() {
        return projectAbstract;
    }

    public void setProjectAbstract(String projectAbstract) {
        this.projectAbstract = projectAbstract;
    }

    @Temporal(TemporalType.TIMESTAMP)
    public Date getTs() {
        return ts;
    }

    public void setTs(Date ts) {
        this.ts = ts;
    }

    @Column(nullable = false)
    public String getValidationXml() {
        return validationXml;
    }

    public void setValidationXml(String validationXml) {
        this.validationXml = validationXml;
    }

    @Column(name="public",
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

        if (this.getProjectId() != 0 && project.getProjectId() != 0)
            return this.getProjectId() == project.getProjectId();

        if (isPublic() != project.isPublic()) return false;
        if (!getProjectCode().equals(project.getProjectCode())) return false;
        if (getProjectTitle() != null ? !getProjectTitle().equals(project.getProjectTitle()) : project.getProjectTitle() != null)
            return false;
        if (getProjectAbstract() != null ? !getProjectAbstract().equals(project.getProjectAbstract()) : project.getProjectAbstract() != null)
            return false;
        return getValidationXml().equals(project.getValidationXml());

    }

    @Override
    public int hashCode() {
        int result = getProjectCode().hashCode();
        result = 31 * result + (getProjectTitle() != null ? getProjectTitle().hashCode() : 0);
        result = 31 * result + (getProjectAbstract() != null ? getProjectAbstract().hashCode() : 0);
        result = 31 * result + getValidationXml().hashCode();
        result = 31 * result + (isPublic() ? 1 : 0);
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
                ", user=" + user +
                '}';
    }

    @OneToMany(mappedBy = "project",
            fetch = FetchType.LAZY
    )
    public Set<Expedition> getExpeditions() {
        return expeditions;
    }

    public void setExpeditions(Set<Expedition> expeditions) {
        this.expeditions = expeditions;
    }

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

    @ManyToMany(mappedBy = "projectsMemberOf",
            fetch = FetchType.LAZY
    )
    public Set<User> getProjectMembers() {
        return projectMembers;
    }

    public void setProjectMembers(Set<User> projectMembers) {
        this.projectMembers = projectMembers;
    }

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
        for (Expedition expedition: getExpeditions()) {
            if (expedition.getExpeditionCode().equals(expeditionCode))
                return expedition;
        }
        return null;
    }
}
