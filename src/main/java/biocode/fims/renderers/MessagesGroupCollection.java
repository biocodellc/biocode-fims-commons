package biocode.fims.renderers;

import java.util.ArrayList;
import java.util.List;

/**
 * @author rjewing
 */
public class MessagesGroupCollection {

    public List<MessagesGroup> messages;

    public MessagesGroupCollection() {
        messages = new ArrayList<>();
    }


    public void addMessage(String groupName, Message message) {
        MessagesGroup messagesGroup = getGroupMessages(groupName);
        messagesGroup.add(message);
    }

    private MessagesGroup getGroupMessages(String name) {
        for (MessagesGroup messagesGroup : messages) {
            if (matchesName(name, messagesGroup)) {
                return messagesGroup;
            }
        }

        return addNewGroupMessage(name);
    }

    private MessagesGroup addNewGroupMessage(String name) {
        MessagesGroup messagesGroup = new MessagesGroup(name);
        messages.add(messagesGroup);
        return messagesGroup;
    }

    private boolean matchesName(String name, MessagesGroup gMsg) {
        return gMsg.getName().equals(name);
    }


    public List<MessagesGroup> allGroupMessages() {
        return messages;
    }

    public MessagesGroup groupMessages(String name) {
        for (MessagesGroup messagesGroup : messages) {
            if (matchesName(name, messagesGroup)) {
                return messagesGroup;
            }
        }
        return new MessagesGroup(name);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof MessagesGroupCollection)) return false;

        MessagesGroupCollection that = (MessagesGroupCollection) o;

        return messages.equals(that.messages);
    }

    @Override
    public int hashCode() {
        return messages.hashCode();
    }
}
