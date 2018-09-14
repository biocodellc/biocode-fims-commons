package biocode.fims.models;

import biocode.fims.config.network.NetworkConfig;
import biocode.fims.models.dataTypes.JsonBinaryType;
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

/**
 * @author rjewing
 */
@JsonIgnoreProperties({ "config" })
@TypeDef(name = "jsonb", typeClass = JsonBinaryType.class)
@Entity
@Table(name = "networks")
public class Network {

    private int id;
    private String title;
    private String description;
    private NetworkConfig networkConfig;
    private Date created;
    private Date modified;
    private User user;

    // needed for hibernate
    Network() {}

    public Network(String title, NetworkConfig networkConfig) {
        this.title = title;
        this.networkConfig = networkConfig;
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

    @JsonView(Views.Summary.class)
    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    @JsonView(Views.Detailed.class)
    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    @JsonProperty("config")
    @JsonView(Views.DetailedConfig.class)
    @Type(type = "jsonb")
    @Column(columnDefinition = "jsonb", name = "config", updatable = false)
    public NetworkConfig getNetworkConfig() {
        return networkConfig;
    }

    public void setNetworkConfig(NetworkConfig networkConfig) {
        this.networkConfig = networkConfig;
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Network)) return false;

        Network network = (Network) o;

        return getId() == network.getId();
    }

    @Override
    public int hashCode() {
        return getId();
    }

    @Override
    public String toString() {
        return "Network{" +
                "id=" + id +
                ", title='" + title + '\'' +
                ", description='" + description + '\'' +
                ", networkConfig=" + networkConfig +
                ", created=" + created +
                ", modified=" + modified +
                ", user=" + user +
                '}';
    }
}
