package biocode.fims.utils;

import biocode.fims.settings.SettingsManager;
import org.json.simple.JSONObject;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.sql.Timestamp;

/**
 * A data class to provide information to the user about exceptions thrown in a service
 */
public class ErrorInfo {
    private Integer httpStatusCode;
    private Object usrMessage;
    private Object developerMessage;
    private Exception e;
    private Timestamp ts;

    public ErrorInfo(String usrMessage, String developerMessage, int httpStatusCode, Exception e) {
        this.httpStatusCode = httpStatusCode;
        this.usrMessage = usrMessage;
        this.developerMessage = developerMessage;
        this.e = e;
        this.ts = new Timestamp(new java.util.Date().getTime());
    }

    public ErrorInfo(String usrMessage, int httpStatusCode) {
        this.httpStatusCode = httpStatusCode;
        this.usrMessage = usrMessage;
        this.ts = new Timestamp(new java.util.Date().getTime());
    }

    public Integer getHttpStatusCode() {
        return httpStatusCode;
    }

    public String getMessage() {
        return usrMessage.toString();
    }

    public String toJSON() {
        JSONObject obj = new JSONObject();
        convertToNull();

        obj.put("usrMessage", usrMessage);
        obj.put("developerMessage", developerMessage);
        obj.put("httpStatusCode", httpStatusCode);
        obj.put("time", ts.toString());

        if (e != null) {
            SettingsManager sm = SettingsManager.getInstance();
            String debug = sm.retrieveValue("debug", "false");

            if (debug.equalsIgnoreCase("true")) {
                obj.put("exceptionMessage", e.getMessage());
                obj.put("stackTrace", getStackTraceString());
            }
        }
        return obj.toString();
    }

    // JSONObject doesn't accept regular null object, so must convert them to ""
    private void convertToNull() {
        if (usrMessage == null) usrMessage = "";
        if (developerMessage == null) developerMessage = "";
    }

    // returns the full stackTrace as a string
    private String getStackTraceString() {
        final StringWriter sw = new StringWriter();
        final PrintWriter pw = new PrintWriter(sw);
        e.printStackTrace(pw);
        return sw.toString();
    }

}
