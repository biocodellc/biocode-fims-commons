package biocode.fims.rest;

/**
 * @author RJ Ewing
 */
public class UrlResponse {
    private final String url;

    public UrlResponse(String url) {
        this.url = url;
    }

    public String url() {
        return url;
    }
}
