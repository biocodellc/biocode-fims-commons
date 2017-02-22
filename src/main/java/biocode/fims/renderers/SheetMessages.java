package biocode.fims.renderers;

import java.util.List;

/**
 * @author rjewing
 */
public class SheetMessages {

    private MessagesGroupCollection errorMessages;
    private MessagesGroupCollection warningMessages;

    public SheetMessages() {
        warningMessages = new MessagesGroupCollection();
        errorMessages = new MessagesGroupCollection();
    }

    public void addErrorMessage(String groupMessage, Message message) {
        errorMessages.addMessage(groupMessage, message);
    }

    public void addWarningMessage(String groupMessage, Message msg) {
        warningMessages.addMessage(groupMessage, msg);
    }

    public List<MessagesGroup> getWarningMessages() {
        return warningMessages.allGroupMessages();
    }

    public List<MessagesGroup> getErrorMessages() {
        return errorMessages.allGroupMessages();
    }

}
