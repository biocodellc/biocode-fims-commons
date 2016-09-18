package biocode.fims.config;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.util.ArrayList;
import java.util.Iterator;

/**
 * Manage configuration file messages, so we can display them easily
 */
public class ConfigurationFileErrorMessager extends ArrayList {
    ArrayList<message> messages = new ArrayList<message>();

    public void add(ConfigurationFileTester tester, String message, String validationName) {
        messages.add(new message(tester, message, validationName));
    }

    /**
     * print all the messages
     * @return
     */
    public String printMessages() {
        Iterator it  = messages.iterator();
        StringBuilder sb = new StringBuilder();
        while (it.hasNext()) {
            message m = (message) it.next();
            sb.append(m.getFullMessage());
        }
        return sb.toString();
    }

    /**
     * returns messages in a format for the web interface parseResults
     * @return
     */
    public JSONObject getMessages() {
        JSONObject messagesObject = new JSONObject();
        JSONObject groupMessage = new JSONObject();
        JSONArray messages = new JSONArray();

        for (message message : this.messages) {
            messages.add(message.getValidationName() + ": " + message.getMessage());
        }

        groupMessage.put("Configuration File", messages);
        messagesObject.put("errors", groupMessage);
        return messagesObject;
    }

    /**
     * Hold message contents
      */
    class message {
        ConfigurationFileTester tester;
        String message;
        String validationName;

        message(ConfigurationFileTester tester, String message, String validationName) {
            this.tester = tester;
            this.message = message;
            this.validationName = validationName;
        }

        public String getFile() {
            return tester.getFileToTest().getName();
        }
        public String getValidationName() {
             return validationName;
        }
        public String getMessage() {
            return message;
        }
        public String getFullMessage() {
            return getFile() + "; " + getValidationName() + "; message = " + getMessage() + "\n";
        }
    }
}
