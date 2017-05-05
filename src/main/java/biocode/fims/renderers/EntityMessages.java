package biocode.fims.renderers;

import java.util.List;

/**
 * @author rjewing
 */
public class EntityMessages {

    private String conceptAlias;
    private String sheetName;
    private MessagesGroupCollection errorMessages;
    private MessagesGroupCollection warningMessages;

    public EntityMessages(String conceptAlias) {
        this.conceptAlias = conceptAlias;
        warningMessages = new MessagesGroupCollection();
        errorMessages = new MessagesGroupCollection();
    }

    public EntityMessages(String conceptAlias, String sheetName) {
        this(conceptAlias);
        this.sheetName = sheetName;
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

    public String conceptAlias() {
        return conceptAlias;
    }
}
