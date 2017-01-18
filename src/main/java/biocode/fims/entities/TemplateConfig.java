package biocode.fims.entities;

import biocode.fims.serializers.JsonViewOverride;
import biocode.fims.serializers.Views;
import com.fasterxml.jackson.annotation.JsonView;

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

    TemplateConfig() {}

    @JsonView(Views.Summary.class)
    @Id
    @GeneratedValue(strategy=GenerationType.IDENTITY)
    public int getTemplateConfigId() {
        return templateConfigId;
    }

    public void setTemplateConfigId(int id) {
        this.templateConfigId = id;
    }

    @JsonView(Views.Summary.class)
    @Column(nullable = false, updatable = false)
    public String getConfigName() {
        return configName;
    }

    public void setConfigName(String configName) {
        this.configName = configName;
    }

    @JsonView(Views.Summary.class)
    @Column(name="public")
    public boolean getIsPublic() {
        return isPublic;
    }

    public void setIsPublic(boolean isPublic) {
        this.isPublic = isPublic;
    }

    @JsonView(Views.Detailed.class)
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
        if (!(o instanceof TemplateConfig)) return false;

        TemplateConfig that = (TemplateConfig) o;

        if (!getConfigName().equals(that.getConfigName())) return false;
        return getProject().equals(that.getProject());

    }

    @Override
    public int hashCode() {
        int result = getConfigName().hashCode();
        result = 31 * result + getProject().hashCode();
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

    @JsonView(Views.Detailed.class)
    @JsonViewOverride(Views.Summary.class)
    @ManyToOne
    @JoinColumn(name = "projectId",
            referencedColumnName = "projectId",
            nullable = false, updatable = false,
            foreignKey = @ForeignKey(name = "FK_templateConfigs_projectId")
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
