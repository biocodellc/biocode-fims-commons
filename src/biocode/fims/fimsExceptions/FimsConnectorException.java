package biocode.fims.fimsExceptions;

import org.json.simple.JSONObject;

/**
 * An exception class to wrap exceptions thrown by the biocode-fims system.
 */
public class FimsConnectorException extends FimsAbstractException {

    public FimsConnectorException(Integer httpStatusCode, Throwable cause) {
        super(httpStatusCode, cause);
        this.httpStatusCode = httpStatusCode;
    }

    public FimsConnectorException(String developerMessage, Integer httpStatusCode, Throwable cause) {
        super(developerMessage, httpStatusCode, cause);
    }

    public FimsConnectorException(JSONObject response) {
        super(response);
    }

    public FimsConnectorException(String developerMessage, Integer httpStatusCode) {
        super(developerMessage, httpStatusCode);
        this.httpStatusCode = httpStatusCode;
        this.developerMessage = developerMessage;
    }

    public Integer getHttpStatusCode() {
        return httpStatusCode;
    }

    public String getUsrMessage() {
        return usrMessage;
    }

    public String getDeveloperMessage() {
        return developerMessage;
    }
}
