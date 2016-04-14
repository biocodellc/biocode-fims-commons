package biocode.fims.entities;

import biocode.fims.bcid.BcidDatabase;
import biocode.fims.repository.ExpeditionRepository;
import org.springframework.beans.factory.annotation.Autowired;

import java.net.URI;
import java.sql.Timestamp;

/**
 * bcid entity object
 */
public class Bcid {
    private Integer bcidId;
    private boolean ezidMade;
    private boolean ezidRequest;
    private URI identifier;
    private int userId;
    private String doi;
    private String title;
    private URI webAddress;
    private String resourceType;
    private Timestamp ts;
    private String graph;
    private boolean finalCopy;

    //TODO add User object

    @Autowired
    private ExpeditionRepository expeditionRepository;
    private Expedition expedition = null;
    private boolean attemptedToFetchExpedition = false;

    public static class BcidBuilder {

        // Required parameters
        private String resourceType;
        private int userId;
        //Optional parameters
        private boolean ezidMade = false;

        private boolean ezidRequest = true;
        private URI identifier;
        private String doi;
        private String title;
        private URI webAddress;
        private String graph;
        private boolean finalCopy = false;
        public BcidBuilder(int userId, String resourceType) {

            this.userId = userId;
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

        public BcidBuilder identifier(URI val) {
            identifier = val;
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

        private void checkEzidRequest() {
            // Never request EZID for user=demo
            if (ezidRequest && BcidDatabase.getUserName(userId).equalsIgnoreCase("demo"))
                ezidRequest = false;
        }

        public Bcid build() {
            checkEzidRequest();
            return new Bcid(this);
        }

    }

    private Bcid(BcidBuilder builder) {
        resourceType = builder.resourceType;
        userId = builder.userId;
        ezidMade = builder.ezidMade;
        ezidRequest = builder.ezidRequest;
        identifier = builder.identifier;
        doi = builder.doi;
        title = builder.title;
        webAddress = builder.webAddress;
        graph = builder.graph;
        finalCopy = builder.finalCopy;
    }

    /**
     * Get the {@link Expedition} this {@link Bcid} belongs to. Null if this {@link Bcid} doesn't
     * belong to an {@link Expedition}
     * @return
     */
    public Expedition getExpedition() {
        if (!attemptedToFetchExpedition && expedition == null) {
            expedition = expeditionRepository.findByBcid(this);
            attemptedToFetchExpedition = true;
        }

        return expedition;
    }

    public boolean isNew() {
        return (this.bcidId == null);
    }

    /**
     * This will only set the bcidId if the current bcidId is null. This method is only to be used when
     * fetching a Bcid from the db
     * @param bcidId
     */
    public void setBcidId(Integer bcidId) {
        if (this.bcidId == null)
            this.bcidId = bcidId;
    }

    public void setEzidMade(boolean ezidMade) {
        this.ezidMade = ezidMade;
    }

    /**
     * This will only set the identifier if the current identifier is null. This method is only to be used when
     * fetching a Bcid from the db
     * @param identifier
     */
    public void setIdentifier(URI identifier) {
        if (this.identifier == null)
            this.identifier = identifier;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public void setDoi(String doi) {
        this.doi = doi;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setWebAddress(URI webAddress) {
        this.webAddress = webAddress;
    }

    public void setResourceType(String resourceType) {
        this.resourceType = resourceType;
    }

    public void setTs(Timestamp ts) { this.ts = ts; }

    public Integer getBcidId() {
        return bcidId;
    }

    public boolean isEzidMade() {
        return ezidMade;
    }

    public boolean isEzidRequest() {
        return ezidRequest;
    }

    public URI getIdentifier() {
        return identifier;
    }

    public int getUserId() {
        return userId;
    }

    public String getDoi() {
        return doi;
    }

    public String getTitle() {
        return title;
    }

    public URI getWebAddress() {
        return webAddress;
    }

    public String getResourceType() {
        return resourceType;
    }

    public Timestamp getTs() {
        return ts;
    }

    public String getGraph() {
        return graph;
    }

    public boolean isFinalCopy() {
        return finalCopy;
    }

    @Override
    public String toString() {
        return "Bcid{" +
                "bcidId=" + bcidId +
                ", ezidMade=" + ezidMade +
                ", ezidRequest=" + ezidRequest +
                ", identifier=" + identifier +
                ", userId=" + userId +
                ", doi='" + doi + '\'' +
                ", title='" + title + '\'' +
                ", webAddress=" + webAddress +
                ", resourceType='" + resourceType + '\'' +
                ", ts=" + ts +
                ", graph='" + graph + '\'' +
                ", finalCopy=" + finalCopy +
                ", expeditionRepository=" + expeditionRepository +
                ", expedition=" + expedition +
                ", attemptedToFetchExpedition=" + attemptedToFetchExpedition +
                '}';
    }
}
