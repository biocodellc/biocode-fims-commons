package biocode.fims.rest.responses;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * @author RJ Ewing
 */
public class UrlResponse {
    private final String url;

    public UrlResponse(String url) {
        this.url = url;
    }

    @JsonProperty()
    public String url() {
        return url;
    }
}
