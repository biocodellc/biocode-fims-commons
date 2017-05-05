package validation;

import biocode.fims.renderers.MessagesGroup;
import biocode.fims.renderers.EntityMessages;
import org.json.simple.JSONObject;

/**
 * @author rjewing
 */
public class EntityMessagesUtils {

    public static JSONObject sheetMessagesToJSONObject(EntityMessages entityMessages) {
        JSONObject messages = new JSONObject();

        JSONObject errors = new JSONObject();
        for (MessagesGroup g : entityMessages.errorMessages()) {
            errors.put(g.getName(), g.messages());
        }

        JSONObject warnings = new JSONObject();
        for (MessagesGroup g : entityMessages.warningMessages()) {
            warnings.put(g.getName(), g.messages());
        }

        messages.put("errors", errors);
        messages.put("warnings", warnings);
        return messages;
    }
}
