package biocode.fims.fimsExceptions;

import biocode.fims.utils.SpringApplicationContext;
import org.json.simple.JSONObject;
import org.springframework.context.MessageSource;

import java.io.PrintWriter;
import java.util.Locale;

/**
 * An abstract exception to be extended by exceptions thrown to return appropriate responses.
 */
public abstract class FimsAbstractException extends RuntimeException {
    private static final MessageSource messageSource = (MessageSource) SpringApplicationContext.getBean("messageSource");
    String usrMessage = "Server Error";
    Integer httpStatusCode;
    String developerMessage;
    ErrorCode errorCode;

    public FimsAbstractException(String usrMessage, Integer httpStatusCode) {
        super();
        this.httpStatusCode = httpStatusCode;
        this.usrMessage = usrMessage;
    }

    public FimsAbstractException(String usrMessage, String developerMessage, Integer httpStatusCode) {
        super(developerMessage);
        this.httpStatusCode = httpStatusCode;
        this.usrMessage = usrMessage;
        this.developerMessage = developerMessage;
    }

    public FimsAbstractException(String usrMessage, String developerMessage, Integer httpStatusCode, Throwable cause) {
        super(developerMessage, cause);
        this.httpStatusCode = httpStatusCode;
        this.usrMessage = usrMessage;
        this.developerMessage = developerMessage;
    }

    public FimsAbstractException(Integer httpStatusCode, Throwable cause) {
        super(cause);
        this.httpStatusCode = httpStatusCode;
    }

    public FimsAbstractException(String developerMessage, Integer httpStatusCode, Throwable cause) {
        super(developerMessage, cause);
        this.httpStatusCode = httpStatusCode;
        this.developerMessage = developerMessage;
    }

    public FimsAbstractException(JSONObject response) {
        super((String) response.get("developerMessage"));
        this.httpStatusCode = ((Long) response.get("httpStatusCode")).intValue();
        this.usrMessage = (String) response.get("usrMessage");
        this.developerMessage = (String) response.get("developerMessage");
    }

    public FimsAbstractException(ErrorCode errorCode, int httpStatusCode, String... messageArgs) {
        super();
        this.errorCode = errorCode;
        this.usrMessage = getUserMessageFromErrorCode(messageArgs);
        this.httpStatusCode = httpStatusCode;
    }

    public FimsAbstractException(ErrorCode errorCode, String developerMessage, int httpStatusCode, String... messageArgs) {
        super(developerMessage);
        this.errorCode = errorCode;
        this.usrMessage = getUserMessageFromErrorCode(messageArgs);
        this.httpStatusCode = httpStatusCode;
    }

    public ErrorCode getErrorCode() {
        return errorCode;
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

    private String getUserMessageFromErrorCode(String... messageArgs) {
        if (errorCode == null) {
            return "Server Error";
        }

        String key = errorCode.getClass().getSimpleName() + "__" + errorCode;
        return messageSource.getMessage(key, messageArgs, Locale.US);
    }

    @Override
    public String toString() {
        return "FimsAbstractException{" +
                " usrMessage='" + usrMessage + '\'' +
                ", errorCode=" + errorCode +
                ", httpStatusCode=" + httpStatusCode +
                ", developerMessage='" + developerMessage + '\'' +
                '}';
    }
}