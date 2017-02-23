package biocode.fims.renderers;

import java.util.List;

/**
 * @author rjewing
 */
public class SheetMessages {

    private String sheetName;
    private MessagesGroupCollection errorMessages;
    private MessagesGroupCollection warningMessages;

    public SheetMessages(String sheetName) {
        this.sheetName = sheetName;
        warningMessages = new MessagesGroupCollection();
        errorMessages = new MessagesGroupCollection();
    }

    public void addErrorMessage(String groupMessage, Message message) {
        errorMessages.addMessage(groupMessage, message);
    }

    public void addWarningMessage(String groupMessage, Message msg) {
        warningMessages.addMessage(groupMessage, msg);
    }

    public List<MessagesGroup> warningMessages() {
        return warningMessages.allGroupMessages();
    }

    public List<MessagesGroup> errorMessages() {
        return errorMessages.allGroupMessages();
    }

    public String sheetName() {
        return sheetName;
    }
}
