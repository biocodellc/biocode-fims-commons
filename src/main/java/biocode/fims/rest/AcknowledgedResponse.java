package biocode.fims.rest;

/**
 * @author RJ Ewing
 */
public class AcknowledgedResponse {
    private final boolean acknowledged;

    public AcknowledgedResponse(boolean acknowledged) {
        this.acknowledged = acknowledged;
    }

    public boolean isAcknowledged() {
        return acknowledged;
    }
}
