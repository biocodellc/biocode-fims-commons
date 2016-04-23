package biocode.fims.intelliJEntities;

import javax.persistence.*;

/**
 * TemplateConfig Entity object
 */
@Entity
@Table(name = "templateConfigs", schema = "biscicol")
public class TemplateConfig {

    private int templateConfigId;
    private String configName;
    private byte isPublic;
    private String config;
    private Project project;
    private User user;

    @Id
    @GeneratedValue(strategy=GenerationType.IDENTITY)
    public int getTemplateConfigId() {
        return templateConfigId;
    }

    public void setTemplateConfigId(int id) {
        this.templateConfigId = id;
    }

    public String getConfigName() {
        return configName;
    }

    public void setConfigName(String configName) {
        this.configName = configName;
    }

    public byte getIsPublic() {
        return isPublic;
    }

    public void setIsPublic(byte isPublic) {
        this.isPublic = isPublic;
    }

    public String getConfig() {
        return config;
    }

    public void setConfig(String config) {
        this.config = config;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        TemplateConfig that = (TemplateConfig) o;

        if (templateConfigId != that.templateConfigId) return false;
        if (isPublic != that.isPublic) return false;
        if (configName != null ? !configName.equals(that.configName) : that.configName != null) return false;
        if (config != null ? !config.equals(that.config) : that.config != null) return false;
        if (project != null ? !project.equals(that.project) : that.project != null) return false;
        return user != null ? user.equals(that.user) : that.user == null;

    }

    @Override
    public int hashCode() {
        int result = templateConfigId;
        result = 31 * result + (configName != null ? configName.hashCode() : 0);
        result = 31 * result + (int) isPublic;
        result = 31 * result + (config != null ? config.hashCode() : 0);
        result = 31 * result + (project != null ? project.hashCode() : 0);
        result = 31 * result + (user != null ? user.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "TemplateConfig{" +
                "templateConfigId=" + templateConfigId +
                ", configName='" + configName + '\'' +
                ", isPublic=" + isPublic +
                ", config='" + config + '\'' +
                ", project=" + project +
                ", user=" + user +
                '}';
    }

    @ManyToOne
    @JoinColumn(name = "projectId",
            referencedColumnName = "projectId",
            nullable = false,
            foreignKey = @ForeignKey(name = "FK_templateConfigs_projectId")
    )
    public Project getProject() {
        return project;
    }

    public void setProject(Project project) {
        this.project = project;
    }

    @ManyToOne
    @JoinColumn(name = "userId",
            referencedColumnName = "userId",
            nullable = false,
            foreignKey = @ForeignKey(name = "FK_templateConfigs_userId")
    )
    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }
}
