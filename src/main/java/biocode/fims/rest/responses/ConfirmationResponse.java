package biocode.fims.rest.responses;

/**
 * @author RJ Ewing
 */
public class ConfirmationResponse {
    private final boolean success;

    public ConfirmationResponse(boolean success) {
        this.success = success;
    }

    public boolean isSuccess() {
        return success;
    }
}
