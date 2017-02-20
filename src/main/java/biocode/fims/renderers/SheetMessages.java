package biocode.fims.renderers;

import java.util.List;

/**
 * @author rjewing
 */
public class SheetMessages {

    private GroupMessagesCollection errorMessages;
    private GroupMessagesCollection warningMessages;

    public SheetMessages() {
        warningMessages = new GroupMessagesCollection();
        errorMessages = new GroupMessagesCollection();
    }

    public void addErrorMessage(String groupMessage, Message message) {
        errorMessages.addMessage(groupMessage, message);
    }

    public void addWarningMessage(String groupMessage, Message msg) {
        warningMessages.addMessage(groupMessage, msg);
    }

    public List<GroupMessages> getWarningMessages() {
        return warningMessages.allGroupMessages();
    }

    public List<GroupMessages> getErrorMessages() {
        return errorMessages.allGroupMessages();
    }

}
