package biocode.fims.validation.messages;

import biocode.fims.validation.rules.RuleLevel;
import com.fasterxml.jackson.annotation.JsonProperty;

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

    public void addMessage(String groupMessage, Message msg, RuleLevel level) {
        if (RuleLevel.ERROR == level) {
            addErrorMessage(groupMessage, msg);
        } else {
            addWarningMessage(groupMessage, msg);
        }
    }

    @JsonProperty("warnings")
    public List<MessagesGroup> warningMessages() {
        return warningMessages.allGroupMessages();
    }

    @JsonProperty("errors")
    public List<MessagesGroup> errorMessages() {
        return errorMessages.allGroupMessages();
    }

    @JsonProperty
    public String sheetName() {
        return sheetName;
    }

    @JsonProperty("queryEntity")
    public String conceptAlias() {
        return conceptAlias;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof EntityMessages)) return false;

        EntityMessages that = (EntityMessages) o;

        if (!conceptAlias.equals(that.conceptAlias)) return false;
        if (sheetName != null ? !sheetName.equals(that.sheetName) : that.sheetName != null) return false;
        if (!errorMessages.equals(that.errorMessages)) return false;
        return warningMessages.equals(that.warningMessages);
    }

    @Override
    public int hashCode() {
        int result = conceptAlias.hashCode();
        result = 31 * result + (sheetName != null ? sheetName.hashCode() : 0);
        result = 31 * result + errorMessages.hashCode();
        result = 31 * result + warningMessages.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return "EntityMessages{" +
                "conceptAlias='" + conceptAlias + '\'' +
                ", sheetName='" + sheetName + '\'' +
                ", errorMessages=" + errorMessages +
                ", warningMessages=" + warningMessages +
                '}';
    }
}
