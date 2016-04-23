package biocode.fims.intelliJEntities;

import javax.persistence.*;
import java.sql.Timestamp;
import java.util.Date;
import java.util.Set;

/**
 * Project Entity object
 */
@Entity
@Table(name = "projects", schema = "biscicol")
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
    private Set<User> projectUsers;
    private Set<TemplateConfig> templateConfigs;

    public static class ProjectBuilder {

        // Required
        private String projectCode;
        private String projectTitle;
        private User user;
        private String validationXml;

        // Optional
        private String projectAbstract;
        private boolean isPublic = true;

        public ProjectBuilder(String projectCode, String projectTitle, User user, String validationXml) {
            this.projectCode = projectCode;
            this.projectTitle = projectTitle;
            this.user = user;
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
        user = builder.user;
        validationXml = builder.validationXml;
        projectAbstract = builder.projectAbstract;
        isPublic = builder.isPublic;
    }

    // needed for hibernate
    private Project() {
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

    public String getValidationXml() {
        return validationXml;
    }

    public void setValidationXml(String validationXml) {
        this.validationXml = validationXml;
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

        Project project = (Project) o;

        if (projectId != project.projectId) return false;
        if (isPublic != project.isPublic) return false;
        if (projectCode != null ? !projectCode.equals(project.projectCode) : project.projectCode != null) return false;
        if (projectTitle != null ? !projectTitle.equals(project.projectTitle) : project.projectTitle != null)
            return false;
        if (projectAbstract != null ? !projectAbstract.equals(project.projectAbstract) : project.projectAbstract != null)
            return false;
        if (ts != null ? !ts.equals(project.ts) : project.ts != null) return false;
        if (validationXml != null ? !validationXml.equals(project.validationXml) : project.validationXml != null)
            return false;

        return true;
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

    @Override
    public int hashCode() {
        int result = projectId;
        result = 31 * result + (projectCode != null ? projectCode.hashCode() : 0);
        result = 31 * result + (projectTitle != null ? projectTitle.hashCode() : 0);
        result = 31 * result + (projectAbstract != null ? projectAbstract.hashCode() : 0);
        result = 31 * result + (ts != null ? ts.hashCode() : 0);
        result = 31 * result + (validationXml != null ? validationXml.hashCode() : 0);
        result = 31 * result + (isPublic ? 1 : 0);
        return result;
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
            foreignKey = @ForeignKey(name = "FK_projects_userId")
    )
    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    @ManyToMany(mappedBy = "memberProjects",
            fetch = FetchType.LAZY
    )
    public Set<User> getProjectUsers() {
        return projectUsers;
    }

    public void setProjectUsers(Set<User> projectUsers) {
        this.projectUsers = projectUsers;
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
}
