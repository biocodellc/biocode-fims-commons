package biocode.fims.entities;

import biocode.fims.converters.UriPersistenceConverter;
import biocode.fims.fimsExceptions.BadRequestException;
import biocode.fims.fimsExceptions.ServerErrorException;
import org.apache.commons.validator.routines.UrlValidator;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;

import javax.persistence.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Date;

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
    private Date ts;
    private String graph;
    private boolean finalCopy;
    private Expedition expedition;
    private User user;

    public static class BcidBuilder {

        // Required parameters
        private String resourceType;
        private User user;
        //Optional parameters
        private boolean ezidMade = false;

        private boolean ezidRequest = true;
        private String doi;
        private String title;
        private URI webAddress;
        private String graph;
        private boolean finalCopy = false;
        private Expedition expedition;

        public BcidBuilder(User user, String resourceType) {
            Assert.notNull(user, "Bcid user must not be null");
            Assert.notNull(resourceType, "Bcid resourceType must not be null");
            this.user = user;
            this.resourceType = resourceType;
        }

        public BcidBuilder ezidMade(boolean val) {
            ezidMade = val;
            return this;
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

        public BcidBuilder finalCopy(boolean finalCopy) {
            this.finalCopy = finalCopy;
            return this;
        }

        public BcidBuilder expedition(Expedition expedition) {
            this.expedition = expedition;
            return this;
        }

        private void checkEzidRequest() {
            // Never request EZID for user=demo
            if (ezidRequest && user.getUsername().equalsIgnoreCase("demo"))
                ezidRequest = false;
        }

        public Bcid build() {
            checkEzidRequest();
            return new Bcid(this);
        }

    }

    private Bcid(BcidBuilder builder) {
        resourceType = builder.resourceType;
        user = builder.user;
        ezidMade = builder.ezidMade;
        ezidRequest = builder.ezidRequest;
        doi = builder.doi;
        title = builder.title;
        webAddress = builder.webAddress;
        graph = builder.graph;
        finalCopy = builder.finalCopy;
        expedition = builder.expedition;
    }

    // needed for hibernate
    private Bcid() {}

    @Id
    @GeneratedValue(strategy=GenerationType.IDENTITY)
    public int getBcidId() {
        return bcidId;
    }

    private void setBcidId(int id) {
        this.bcidId = id;
    }

    public boolean isEzidMade() {
        return ezidMade;
    }

    public void setEzidMade(boolean ezidMade) {
        this.ezidMade = ezidMade;
    }

    @Column(nullable = false)
    public boolean isEzidRequest() {
        return ezidRequest;
    }

    public void setEzidRequest(boolean ezidRequest) {
        this.ezidRequest = ezidRequest;
    }

    @Convert(converter = UriPersistenceConverter.class)
    public URI getIdentifier() {
        return identifier;
    }

    public void setIdentifier(URI identifier) {
        if (this.identifier == null)
            this.identifier = identifier;
    }

    public String getDoi() {
        return doi;
    }

    public void setDoi(String doi) {
        this.doi = doi;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    @Convert(converter = UriPersistenceConverter.class)
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

    public void setWebAddress(URI webAddress) {
        isValidUrl(webAddress);
        this.webAddress = webAddress;
    }

    @Column(nullable = false)
    public String getResourceType() {
        return resourceType;
    }

    public void setResourceType(String resourceType) {
        this.resourceType = resourceType;
    }

    @Temporal(TemporalType.TIMESTAMP)
    public Date getTs() {
        return ts;
    }

    public void setTs(Date ts) {
        this.ts = ts;
    }

    public String getGraph() {
        return graph;
    }

    public void setGraph(String graph) {
        this.graph = graph;
    }

    @Column(nullable = false)
    public boolean isFinalCopy() {
        return finalCopy;
    }

    public void setFinalCopy(boolean finalCopy) {
        this.finalCopy = finalCopy;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Bcid bcid = (Bcid) o;

        if (bcidId != bcid.bcidId) return false;
        if (ezidMade != bcid.ezidMade) return false;
        if (ezidRequest != bcid.ezidRequest) return false;
        if (finalCopy != bcid.finalCopy) return false;
        if (identifier != null ? !identifier.equals(bcid.identifier) : bcid.identifier != null) return false;
        if (doi != null ? !doi.equals(bcid.doi) : bcid.doi != null) return false;
        if (title != null ? !title.equals(bcid.title) : bcid.title != null) return false;
        if (webAddress != null ? !webAddress.equals(bcid.webAddress) : bcid.webAddress != null) return false;
        if (resourceType != null ? !resourceType.equals(bcid.resourceType) : bcid.resourceType != null) return false;
        if (ts != null ? !ts.equals(bcid.ts) : bcid.ts != null) return false;
        if (graph != null ? !graph.equals(bcid.graph) : bcid.graph != null) return false;

        return true;
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
                ", ts=" + ts +
                ", graph='" + graph + '\'' +
                ", finalCopy=" + finalCopy +
                ", expedition=" + expedition +
                ", user=" + user +
                '}';
    }

    @Override
    public int hashCode() {
        int result = bcidId;
        result = 31 * result + (ezidMade ? 1 : 0);
        result = 31 * result + (ezidRequest ? 1 : 0);
        result = 31 * result + (identifier != null ? identifier.hashCode() : 0);
        result = 31 * result + (doi != null ? doi.hashCode() : 0);
        result = 31 * result + (title != null ? title.hashCode() : 0);
        result = 31 * result + (webAddress != null ? webAddress.hashCode() : 0);
        result = 31 * result + (resourceType != null ? resourceType.hashCode() : 0);
        result = 31 * result + (ts != null ? ts.hashCode() : 0);
        result = 31 * result + (graph != null ? graph.hashCode() : 0);
        result = 31 * result + (finalCopy ? 1 : 0);
        return result;
    }

    @ManyToOne
    @JoinTable(name = "expeditionBcids",
            joinColumns = @JoinColumn(name = "bcidId", referencedColumnName = "bcidId"),
            inverseJoinColumns = @JoinColumn(name = "expeditionId", referencedColumnName = "expeditionId"),
            foreignKey = @ForeignKey(name = "FK_expeditionBcids_bcidId"),
            inverseForeignKey = @ForeignKey(name = "FK_expeditionBcids_expedition_id")
    )
    public Expedition getExpedition() {
        return expedition;
    }

    private void setExpedition(Expedition expedition) {
        this.expedition = expedition;
    }

    @ManyToOne
    @JoinColumn(name = "userId",
            referencedColumnName = "userId",
            foreignKey = @ForeignKey(name = "FK_bcids_userId"),
            nullable = false
    )
    public User getUser() {
        return user;
    }

    private void setUser(User user) {
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
