package biocode.fims.models;

import biocode.fims.models.dataTypes.converters.UriPersistenceConverter;
import biocode.fims.fimsExceptions.BadRequestException;
import biocode.fims.fimsExceptions.ServerErrorException;
import biocode.fims.serializers.JsonViewOverride;
import biocode.fims.serializers.Views;
import com.fasterxml.jackson.annotation.JsonView;
import org.apache.commons.validator.routines.UrlValidator;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import javax.persistence.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Date;
import java.util.Objects;

/**
 * Bcid Entity object
 */
@Entity
@Table(name = "bcids")
public class Bcid {
    private int bcidId;
    private boolean ezidMade;
    private boolean ezidRequest;
    private URI identifier;
    private String doi;
    private String title;
    private URI webAddress;
    private String resourceType;
    private String subResourceType;
    private Date created;
    private Date modified;
    private String graph;
    private String sourceFile;
    private Expedition expedition;
    private User user;

    public static class BcidBuilder {

        // Required parameters
        private String resourceType;
        //Optional parameters
        private boolean ezidMade = false;

        private String subResourceType;
        private boolean ezidRequest = true;
        private String doi;
        private String title;
        private URI webAddress;
        private String graph;
        private String sourceFile;

        public BcidBuilder(String resourceType) {
            Assert.notNull(resourceType, "Bcid resourceType must not be null");
            this.resourceType = resourceType;
        }

        public BcidBuilder ezidRequest(boolean val) {
            ezidRequest = val;
            return this;
        }

        public BcidBuilder doi(String val) {
            doi = val;
            return this;
        }

        public BcidBuilder title(String val) {
            title = val;
            return this;
        }

        public BcidBuilder webAddress(URI val) {
            isValidUrl(val);
            webAddress = val;
            return this;
        }

        public BcidBuilder graph(String val) {
            graph = val;
            return this;
        }

        public BcidBuilder sourceFile(String sourceFile) {
            this.sourceFile = sourceFile;
            return this;
        }

        public BcidBuilder subResourceType(String subResourceType) {
            this.subResourceType = subResourceType;
            return this;
        }

        public Bcid build() {
            return new Bcid(this);
        }

    }

    private Bcid(BcidBuilder builder) {
        resourceType = builder.resourceType;
        ezidMade = builder.ezidMade;
        ezidRequest = builder.ezidRequest;
        doi = builder.doi;
        title = builder.title;
        webAddress = builder.webAddress;
        graph = builder.graph;
        sourceFile = builder.sourceFile;
        subResourceType = builder.subResourceType;
    }

    // needed for hibernate
    Bcid() {
    }

    @JsonView(Views.Detailed.class)
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    public int getBcidId() {
        return bcidId;
    }

    private void setBcidId(int id) {
        this.bcidId = id;
    }

    @JsonView(Views.Detailed.class)
    @Column(columnDefinition = "bit", name = "ezid_made")
    public boolean isEzidMade() {
        return ezidMade;
    }

    public void setEzidMade(boolean ezidMade) {
        this.ezidMade = ezidMade;
    }

    @JsonView(Views.Summary.class)
    @Column(columnDefinition = "bit not null", name = "ezid_request")
    public boolean isEzidRequest() {
        return ezidRequest;
    }

    public void setEzidRequest(boolean ezidRequest) {
        this.ezidRequest = ezidRequest;
    }

    @JsonView(Views.Summary.class)
    @Convert(converter = UriPersistenceConverter.class)
    public URI getIdentifier() {
        return identifier;
    }

    public void setIdentifier(URI identifier) {
        if (this.identifier == null)
            this.identifier = identifier;
    }

    @JsonView(Views.Detailed.class)
    public String getDoi() {
        return doi;
    }

    public void setDoi(String doi) {
        this.doi = doi;
    }

    @JsonView(Views.Detailed.class)
    @Column(columnDefinition = "text")
    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    @JsonView(Views.Detailed.class)
    @Convert(converter = UriPersistenceConverter.class)
    @Column(name = "web_address")
    public URI getWebAddress() {
        // TODO move the following to the BcidService.create after all Bcid creation is done via BcidService class
        if (identifier != null && webAddress != null && webAddress.toString().contains("%7Bark%7D")) {
            try {
                webAddress = new URI(StringUtils.replace(
                        webAddress.toString(),
                        "%7Bark%7D",
                        identifier.toString()));
            } catch (URISyntaxException e) {
                throw new ServerErrorException(e);
            }
        }
        return webAddress;
    }

    @JsonView(Views.Summary.class)
    @Column(name = "sub_resource_type")
    public String getSubResourceType() {
        return subResourceType;
    }

    public void setSubResourceType(String subResourceType) {
        this.subResourceType = subResourceType;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Bcid)) return false;

        Bcid bcid = (Bcid) o;
        return getIdentifier() != null && Objects.equals(getIdentifier(), bcid.getIdentifier());
    }

    @Override
    public int hashCode() {
        return 31;
    }

    public void setWebAddress(URI webAddress) {
        isValidUrl(webAddress);
        this.webAddress = webAddress;
    }

    @JsonView(Views.Summary.class)
    @Column(nullable = false, name = "resource_type")
    public String getResourceType() {
        return resourceType;
    }

    public void setResourceType(String resourceType) {
        this.resourceType = resourceType;
    }

    @JsonView(Views.Summary.class)
    @Temporal(TemporalType.TIMESTAMP)
    public Date getModified() {
        return modified;
    }

    public void setModified(Date modified) {
        this.modified = modified;
    }

    @Column(updatable = false)
    @JsonView(Views.Summary.class)
    @Temporal(TemporalType.TIMESTAMP)
    public Date getCreated() {
        return created;
    }

    private void setCreated(Date created) {
        this.created = created;
    }

    @JsonView(Views.Detailed.class)
    public String getGraph() {
        return graph;
    }

    public void setGraph(String graph) {
        this.graph = graph;
    }

    @JsonView(Views.Detailed.class)
    @Column(name="source_file")
    public String getSourceFile() {
        return sourceFile;
    }

    public void setSourceFile(String sourceFile) {
        this.sourceFile = sourceFile;
    }

    @Override
    public String toString() {
        return "Bcid{" +
                "bcidId=" + bcidId +
                ", ezidMade=" + ezidMade +
                ", ezidRequest=" + ezidRequest +
                ", identifier='" + identifier + '\'' +
                ", doi='" + doi + '\'' +
                ", title='" + title + '\'' +
                ", webAddress='" + webAddress + '\'' +
                ", resourceType='" + resourceType + '\'' +
                ", subResourceType='" + subResourceType + '\'' +
                ", created=" + created +
                ", modified=" + modified +
                ", graph='" + graph + '\'' +
                ", expedition=" + expedition +
                ", user=" + user +
                '}';
    }

    @JsonView(Views.Detailed.class)
    @JsonViewOverride(Views.Summary.class)
    @ManyToOne
    @JoinTable(name = "expedition_bcids",
            joinColumns = @JoinColumn(name = "bcid_id", referencedColumnName = "id"),
            inverseJoinColumns = @JoinColumn(name = "expedition_id", referencedColumnName = "id"),
            foreignKey = @ForeignKey(name = "FK_expedition_bcids_bcid_id"),
            inverseForeignKey = @ForeignKey(name = "FK_expedition_bcids_expedition_id")
    )
    public Expedition getExpedition() {
        return expedition;
    }

    public void setExpedition(Expedition expedition) {
        this.expedition = expedition;
    }

    @JsonView(Views.Detailed.class)
    @JsonViewOverride(Views.Summary.class)
    @ManyToOne
    @JoinColumn(name = "user_id",
            referencedColumnName = "id",
            foreignKey = @ForeignKey(name = "FK_bcids_userId"),
            nullable = false
    )
    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        Assert.notNull(user, "Bcid user must not be null");
        this.user = user;
    }

    private static void isValidUrl(URI webAddress) {
        if (webAddress != null) {
            String[] schemes = {"http", "https"};
            UrlValidator urlValidator = new UrlValidator(schemes);
            if (!urlValidator.isValid(String.valueOf(webAddress)))
                throw new BadRequestException("Invalid URL for bcid webAddress");
        }
    }
}
