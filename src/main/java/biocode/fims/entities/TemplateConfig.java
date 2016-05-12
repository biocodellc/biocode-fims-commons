package biocode.fims.entities;

import javax.persistence.*;

/**
 * TemplateConfig Entity object
 */
@Entity
@Table(name = "templateConfigs")
public class TemplateConfig {

    private int templateConfigId;
    private String configName;
    private boolean isPublic;
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

    @Column(nullable = false)
    public String getConfigName() {
        return configName;
    }

    public void setConfigName(String configName) {
        this.configName = configName;
    }

    @Column(name="public")
    public boolean getIsPublic() {
        return isPublic;
    }

    public void setIsPublic(boolean isPublic) {
        this.isPublic = isPublic;
    }

    @Column(columnDefinition = "mediumtext not null")
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

        if (getTemplateConfigId() != that.getTemplateConfigId()) return false;
        if (isPublic != that.isPublic) return false;
        if (!getConfigName().equals(that.getConfigName())) return false;
        return getConfig().equals(that.getConfig());

    }

    @Override
    public int hashCode() {
        int result = getTemplateConfigId();
        result = 31 * result + getConfigName().hashCode();
        result = 31 * result + (isPublic ? 1 : 0);
        result = 31 * result + getConfig().hashCode();
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
