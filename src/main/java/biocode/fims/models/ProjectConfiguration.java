package biocode.fims.models;

import biocode.fims.config.project.ProjectConfig;
import biocode.fims.config.project.models.PersistedProjectConfig;
import biocode.fims.models.dataTypes.JsonBinaryType;
import biocode.fims.serializers.JsonViewOverride;
import biocode.fims.serializers.Views;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonView;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;

import javax.persistence.*;
import java.util.Date;
import java.util.Objects;

/**
 * @author rjewing
 */
@TypeDef(name = "jsonb", typeClass = JsonBinaryType.class)
@Entity
@Table(name = "project_configurations")
public class ProjectConfiguration {

    private int id;
    private Date created;
    private Date modified;
    private String name;
    private String description;
    private ProjectConfig projectConfig;
    private PersistedProjectConfig persistedProjectConfig;
    private boolean networkApproved = false;
    private boolean configChanged = false;
    private User user;
    private Network network;

    public ProjectConfiguration(String name, ProjectConfig projectConfig, Network network) {
        this.name = name;
        this.projectConfig = projectConfig;
        this.network = network;
        this.persistedProjectConfig = PersistedProjectConfig.fromProjectConfig(projectConfig);
        configChanged = true;
    }

    // needed for hibernate
    ProjectConfiguration() {
    }

    @JsonView(Views.Summary.class)
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    @Column(updatable = false)
    @JsonView(Views.Detailed.class)
    @Temporal(TemporalType.TIMESTAMP)
    public Date getCreated() {
        return created;
    }

    public void setCreated(Date created) {
        this.created = created;
    }

    @Column(updatable = false)
    @JsonView(Views.Detailed.class)
    @Temporal(TemporalType.TIMESTAMP)
    public Date getModified() {
        return modified;
    }

    public void setModified(Date modified) {
        this.modified = modified;
    }

    @JsonView(Views.Summary.class)
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @JsonView(Views.Summary.class)
    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    @JsonIgnore
    @Type(type = "jsonb")
    @Column(columnDefinition = "jsonb", name = "config")
    public PersistedProjectConfig getPersistedProjectConfig() {
        return persistedProjectConfig;
    }

    public void setPersistedProjectConfig(PersistedProjectConfig persistedProjectConfig) {
        this.persistedProjectConfig = persistedProjectConfig;
    }

    //    @JsonIgnore
    @JsonProperty("config")
    @JsonView(Views.Detailed.class)
    @Transient
    public ProjectConfig getProjectConfig() {
        if (projectConfig == null) {
            projectConfig = persistedProjectConfig.toProjectConfig(network.getNetworkConfig());
        }
        return projectConfig;
    }

    public void setProjectConfig(ProjectConfig projectConfig) {
        if (!Objects.equals(projectConfig, this.projectConfig)) {
            this.projectConfig = projectConfig;
            this.persistedProjectConfig = PersistedProjectConfig.fromProjectConfig(projectConfig);
            configChanged = true;
        }
    }

    @JsonIgnore
    public boolean hasConfigChanged() {
        return configChanged;
    }

    @JsonView(Views.Summary.class)
    @Column(name = "network_approved")
    public boolean isNetworkApproved() {
        return networkApproved;
    }

    public void setNetworkApproved(boolean networkApproved) {
        this.networkApproved = networkApproved;
    }

    @JsonView(Views.Summary.class)
    @JsonViewOverride(Views.Summary.class)
    @ManyToOne
    @JoinColumn(name = "user_id",
            referencedColumnName = "id"
    )
    public User getUser() {
        return user;
    }

    @JsonIgnore
    public void setUser(User user) {
        this.user = user;
    }

    @JsonView(Views.Detailed.class)
    @JsonViewOverride(Views.Summary.class)
    @ManyToOne
    @JoinColumn(name = "network_id",
            referencedColumnName = "id"
    )
    public Network getNetwork() {
        return network;
    }

    @JsonIgnore
    public void setNetwork(Network network) {
        this.network = network;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ProjectConfiguration)) return false;

        ProjectConfiguration that = (ProjectConfiguration) o;

        return getId() == that.getId();
    }

    @Override
    public int hashCode() {
        return getId();
    }
}
