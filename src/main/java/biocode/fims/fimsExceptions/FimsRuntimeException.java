package biocode.fims.fimsExceptions;

import org.json.simple.JSONObject;

/**
 * An exception class to wrap exceptions thrown by the biocode-fims system.
 */
public class FimsRuntimeException extends FimsAbstractException {

    public FimsRuntimeException(String usrMessage, Integer httpStatusCode) {
        super(usrMessage, httpStatusCode);
    }

    public FimsRuntimeException(String usrMessage, String developerMessage, Integer httpStatusCode) {
        super(usrMessage, developerMessage, httpStatusCode);
    }

    public FimsRuntimeException(String usrMessage, String developerMessage, Integer httpStatusCode, Throwable cause) {
        super(usrMessage, developerMessage, httpStatusCode, cause);
    }

    public FimsRuntimeException(Integer httpStatusCode, Throwable cause) {
        super(httpStatusCode, cause);
    }

    public FimsRuntimeException(String developerMessage, Integer httpStatusCode, Throwable cause) {
        super(developerMessage, httpStatusCode, cause);
    }

    public FimsRuntimeException(ErrorCode errorCode, int httpStatusCode, String... messageArgs) {
        super(errorCode, httpStatusCode, messageArgs);
    }

    public FimsRuntimeException(ErrorCode errorCode, String developerMessage, int httpStatusCode, String... messageArgs) {
        super(errorCode, developerMessage, httpStatusCode, messageArgs);
    }

    public FimsRuntimeException(JSONObject response) {
        super(response);
    }

}
