package biocode.fims.bcid;

import biocode.fims.entities.BcidTmp;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.validator.routines.UrlValidator;
import org.springframework.util.Assert;

import javax.ws.rs.BadRequestException;
import java.net.URI;
import java.util.Date;
import java.util.UUID;

/**
 * Bcid Entity object
 */
public class Bcid {
    private boolean ezidMade;
    private boolean ezidRequest;
    private URI identifier;
    private String doi;
    private String title;
    private URI webAddress;
    private String resourceType;
    private Date created;
    private Date modified;
    private UUID userId;
    private String creator;
    private int client_id;


    private Bcid(BcidBuilder builder) {
        resourceType = builder.resourceType;
        ezidRequest = builder.ezidRequest;
        doi = builder.doi;
        title = builder.title;
        webAddress = builder.webAddress;
        creator = builder.creator;
        userId = builder.userId;
    }

    // needed for Jackson
    Bcid() {
    }

    public boolean ezidMade() {
        return ezidMade;
    }

    public boolean ezidRequest() {
        return ezidRequest;
    }

    public void setEzidRequest(boolean ezidRequest) {
        this.ezidRequest = ezidRequest;
    }

    public URI identifier() {
        return identifier;
    }

    public String doi() {
        return doi;
    }

    public String title() {
        return title;
    }

    public URI webAddress() {
        return webAddress;
    }

    public void setWebAddress(URI webAddress) {
        isValidUrl(webAddress);
        this.webAddress = webAddress;
    }

    public String resourceType() {
        return resourceType;
    }

    public UUID userId() {
        return userId;
    }

    public String creator() {
        return creator;
    }

    public Date modified() {
        return modified;
    }

    public Date created() {
        return created;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Bcid)) return false;

        Bcid bcid = (Bcid) o;

        return identifier != null ? identifier.equals(bcid.identifier) : bcid.identifier == null;
    }

    @Override
    public int hashCode() {
        return identifier != null ? identifier.hashCode() : 0;
    }


    private static void isValidUrl(URI webAddress) {
        if (webAddress != null) {
            String[] schemes = {"http", "https"};
            UrlValidator urlValidator = new UrlValidator(schemes);
            if (!urlValidator.isValid(String.valueOf(webAddress)))
                throw new BadRequestException("Invalid URL for bcid webAddress");
        }
    }

    public static Bcid fromBcidTmp(BcidTmp bcidTmp, String creator) {
        // TODO check creator prop which will override the user setting
        return new BcidBuilder(bcidTmp.getResourceType(), creator)
                .doi(bcidTmp.getDoi())
                .ezidRequest(bcidTmp.isEzidRequest())
                .title(bcidTmp.getTitle())
                .webAddress(bcidTmp.getWebAddress())
                .userId(bcidTmp.getUser().getUUID())
                .build();
    }

    public static class BcidBuilder {

        // Required parameters
        private String resourceType;
        private String creator;

        //Optional parameters
        private boolean ezidRequest = true;
        private String doi;
        private String title;
        private URI webAddress;
        private UUID userId;

        public BcidBuilder(String resourceType, String creator) {
            Assert.notNull(resourceType, "Bcid resourceType must not be null");
            Assert.notNull(creator, "Bcid creator must not be null");

            this.resourceType = resourceType;
            this.creator = creator;
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

        public BcidBuilder userId(UUID val) {
            userId = val;
            return this;
        }

        public Bcid build() {
            if (StringUtils.isEmpty(title)) {
                title = resourceType;
            }
            return new Bcid(this);
        }

    }
}
