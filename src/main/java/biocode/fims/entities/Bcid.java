package biocode.fims.entities;

import biocode.fims.converters.UriPersistenceConverter;
import biocode.fims.fimsExceptions.BadRequestException;
import biocode.fims.fimsExceptions.ServerErrorException;
import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.JsonIdentityReference;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;
import org.apache.commons.validator.routines.UrlValidator;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

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
    private String sourceFile;
    private boolean finalCopy;
    private Expedition expedition;
    private User user;

    public static class BcidBuilder {

        // Required parameters
        private String resourceType;
        //Optional parameters
        private boolean ezidMade = false;

        private boolean ezidRequest = true;
        private String doi;
        private String title;
        private URI webAddress;
        private String graph;
        private String sourceFile;
        private boolean finalCopy = false;

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

        public BcidBuilder finalCopy(boolean finalCopy) {
            this.finalCopy = finalCopy;
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
        finalCopy = builder.finalCopy;
    }

    // needed for hibernate
    Bcid() {}

    @Id
    @GeneratedValue(strategy=GenerationType.IDENTITY)
    public int getBcidId() {
        return bcidId;
    }

    private void setBcidId(int id) {
        this.bcidId = id;
    }

    @Column(columnDefinition = "bit")
    public boolean isEzidMade() {
        return ezidMade;
    }

    public void setEzidMade(boolean ezidMade) {
        this.ezidMade = ezidMade;
    }

    @Column(columnDefinition ="bit not null")
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

    @Column(columnDefinition = "text")
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Bcid)) return false;

        Bcid bcid = (Bcid) o;

        if (this.getBcidId() != 0 && bcid.getBcidId() != 0)
            return this.getBcidId() == bcid.getBcidId();

        if (isEzidMade() != bcid.isEzidMade()) return false;
        if (isEzidRequest() != bcid.isEzidRequest()) return false;
        if (isFinalCopy() != bcid.isFinalCopy()) return false;
        if (getIdentifier() != null ? !getIdentifier().equals(bcid.getIdentifier()) : bcid.getIdentifier() != null)
            return false;
        if (getDoi() != null ? !getDoi().equals(bcid.getDoi()) : bcid.getDoi() != null) return false;
        if (getTitle() != null ? !getTitle().equals(bcid.getTitle()) : bcid.getTitle() != null) return false;
        if (getWebAddress() != null ? !getWebAddress().equals(bcid.getWebAddress()) : bcid.getWebAddress() != null)
            return false;
        if (!getResourceType().equals(bcid.getResourceType())) return false;
        if (getGraph() != null ? !getGraph().equals(bcid.getGraph()) : bcid.getGraph() != null) return false;
        return getUser().equals(bcid.getUser());

    }

    @Override
    public int hashCode() {
        int result = (isEzidMade() ? 1 : 0);
        result = 31 * result + (isEzidRequest() ? 1 : 0);
        result = 31 * result + (getIdentifier() != null ? getIdentifier().hashCode() : 0);
        result = 31 * result + (getDoi() != null ? getDoi().hashCode() : 0);
        result = 31 * result + (getTitle() != null ? getTitle().hashCode() : 0);
        result = 31 * result + (getWebAddress() != null ? getWebAddress().hashCode() : 0);
        result = 31 * result + getResourceType().hashCode();
        result = 31 * result + (getGraph() != null ? getGraph().hashCode() : 0);
        result = 31 * result + (isFinalCopy() ? 1 : 0);
        result = 31 * result + getUser().hashCode();
        return result;
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

    public String getSourceFile() {
        return sourceFile;
    }

    public void setSourceFile(String sourceFile) {
        this.sourceFile = sourceFile;
    }

    @Column(columnDefinition = "bit not null")
    public boolean isFinalCopy() {
        return finalCopy;
    }

    public void setFinalCopy(boolean finalCopy) {
        this.finalCopy = finalCopy;
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

    @ManyToOne
    @JsonIdentityInfo(generator = ObjectIdGenerators.PropertyGenerator.class, property="expeditionId")
    @JsonIdentityReference(alwaysAsId = true)
    @JoinTable(name = "expeditionBcids",
            joinColumns = @JoinColumn(name = "bcidId", referencedColumnName = "bcidId"),
            inverseJoinColumns = @JoinColumn(name = "expeditionId", referencedColumnName = "expeditionId"),
            foreignKey = @ForeignKey(name = "FK_expeditionBcids_bcidId"),
            inverseForeignKey = @ForeignKey(name = "FK_expeditionBcids_expedition_id")
    )
    public Expedition getExpedition() {
        return expedition;
    }

    public void setExpedition(Expedition expedition) {
        this.expedition = expedition;
    }

    @ManyToOne
    @JsonIdentityInfo(generator = ObjectIdGenerators.PropertyGenerator.class, property="userId")
    @JsonIdentityReference(alwaysAsId = true)
    @JoinColumn(name = "userId",
            referencedColumnName = "userId",
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
