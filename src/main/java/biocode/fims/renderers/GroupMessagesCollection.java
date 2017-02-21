package biocode.fims.renderers;

import java.util.ArrayList;
import java.util.List;

/**
 * @author rjewing
 */
public class GroupMessagesCollection {

    public List<GroupMessages> messages;

    public GroupMessagesCollection() {
        messages = new ArrayList<>();
    }


    public void addMessage(String groupName, Message message) {
        GroupMessages groupMessages = getGroupMessages(groupName);
        groupMessages.add(message);
    }

    private GroupMessages getGroupMessages(String name) {
        for (GroupMessages groupMessages: messages) {
            if (matchesName(name, groupMessages)) {
                return groupMessages;
            }
        }

        return addNewGroupMessage(name);
    }

    private GroupMessages addNewGroupMessage(String name) {
        GroupMessages groupMessages = new GroupMessages(name);
        messages.add(groupMessages);
        return groupMessages;
    }

    private boolean matchesName(String name, GroupMessages gMsg) {
        return gMsg.getName().equals(name);
    }


    public List<GroupMessages> allGroupMessages() {
        return messages;
    }

    public GroupMessages groupMessages(String name) {
        for (GroupMessages groupMessages: messages) {
            if (matchesName(name, groupMessages)) {
                return groupMessages;
            }
        }
        return new GroupMessages(name);
    }
}
