package biocode.fims.rest;

/**
 * @author RJ Ewing
 */
public class ConformationResponse {
    private final boolean success;

    public ConformationResponse(boolean sucess) {
        this.success = sucess;
    }

    public boolean isSuccess() {
        return success;
    }
}
