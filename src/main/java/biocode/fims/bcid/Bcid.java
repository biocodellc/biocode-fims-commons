package biocode.fims.bcid;

import biocode.fims.models.User;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.validator.routines.UrlValidator;
import org.springframework.util.Assert;

import javax.ws.rs.BadRequestException;
import java.net.URI;

/**
 * Bcid Entity object
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY, getterVisibility = JsonAutoDetect.Visibility.NONE)
public class Bcid {
    private boolean ezidRequest;
    private URI identifier;
    private String title;
    private URI webAddress;
    private String resourceType;
    private String creator;
    private String publisher;


    private Bcid(Bcid.BcidBuilder builder) {
        resourceType = builder.resourceType;
        ezidRequest = builder.ezidRequest;
        title = builder.title;
        webAddress = builder.webAddress;
        publisher = builder.publisher;
        setCreator(builder.user, builder.creatorOverride);
    }

    // needed for Jackson
    Bcid() {
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

    public String creator() {
        return creator;
    }

    public String publisher() {
        return publisher;
    }

    public void setCreator(User user, String creatorProperty) {
        this.creator = (StringUtils.isEmpty(creatorProperty)) ? user.getFullName() + " <" + user.getEmail() + ">" : creatorProperty;
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

    public static class BcidBuilder {

        // Required parameters
        private String resourceType;
        private User user;
        private String creatorOverride;
        private String publisher;

        //Optional parameters
        private boolean ezidRequest = true;
        private String title;
        private URI webAddress;

        public BcidBuilder(String resourceType, String publisher) {
            Assert.notNull(resourceType, "Bcid resourceType must not be null");
            Assert.notNull(publisher, "Bcid publisher must not be null");

            this.resourceType = resourceType;
            this.publisher = publisher;
        }

        public BcidBuilder creator(User user, String creatorOverride) {
            this.user = user;
            this.creatorOverride = creatorOverride;
            return this;
        }

        public BcidBuilder ezidRequest(boolean val) {
            ezidRequest = val;
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

        public Bcid build() {
            if (StringUtils.isEmpty(title)) {
                title = resourceType;
            }
            return new Bcid(this);
        }

    }
}