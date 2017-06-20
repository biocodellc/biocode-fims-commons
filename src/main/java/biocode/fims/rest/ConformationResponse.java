package biocode.fims.rest;

/**
 * @author RJ Ewing
 */
public class ConformationResponse {
    private final boolean success;

    public ConformationResponse(boolean success) {
        this.success = success;
    }

    public boolean isSuccess() {
        return success;
    }
}
