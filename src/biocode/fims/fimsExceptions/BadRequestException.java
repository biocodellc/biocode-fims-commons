package biocode.fims.fimsExceptions;

import javax.ws.rs.core.Response;

/**
 * An exception that encapsulates bad requests
 */
public class BadRequestException extends FimsAbstractException {
    private static Integer httpStatusCode = Response.Status.BAD_REQUEST.getStatusCode();

    public BadRequestException(String usrMessage) {
        super(usrMessage,httpStatusCode);
    }

    public BadRequestException(String usrMessage, Exception e) {
        super(usrMessage, "", httpStatusCode, e);
    }

    public BadRequestException(String usrMessage, String developerMessage) {
        super(usrMessage, developerMessage,httpStatusCode);
    }
}
