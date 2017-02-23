package validation;

import biocode.fims.renderers.MessagesGroup;
import biocode.fims.renderers.SheetMessages;
import org.json.simple.JSONObject;

/**
 * @author rjewing
 */
public class SheetMessagesUtils {

    public static JSONObject sheetMessagesToJSONObject(SheetMessages sheetMessages) {
        JSONObject messages = new JSONObject();

        JSONObject errors = new JSONObject();
        for (MessagesGroup g : sheetMessages.errorMessages()) {
            errors.put(g.getName(), g.messages());
        }

        JSONObject warnings = new JSONObject();
        for (MessagesGroup g : sheetMessages.warningMessages()) {
            warnings.put(g.getName(), g.messages());
        }

        messages.put("errors", errors);
        messages.put("warnings", warnings);
        return messages;
    }
}
