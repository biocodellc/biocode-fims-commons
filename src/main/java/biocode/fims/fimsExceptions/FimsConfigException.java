package biocode.fims.fimsExceptions;

import javax.ws.rs.core.Response;

/**
 * An exception for dealing with Fims Project Config files
 */
public class FimsConfigException extends FimsAbstractException {
    private static Integer httpStatusCode = Response.Status.INTERNAL_SERVER_ERROR.getStatusCode();

    public FimsConfigException(String usrMessage) {
        super(usrMessage, httpStatusCode);
    }

    public FimsConfigException(String usrMessage, Exception e) {
        super(usrMessage, "", httpStatusCode, e);
    }

    public FimsConfigException(String usrMessage, String developerMessage) {
        super(usrMessage, developerMessage, httpStatusCode);
    }
}
