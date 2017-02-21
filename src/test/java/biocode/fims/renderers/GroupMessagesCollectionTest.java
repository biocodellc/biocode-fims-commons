package biocode.fims.renderers;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * @author rjewing
 */
public class GroupMessagesCollectionTest {

    private GroupMessagesCollection messages;

    @Before
    public void setUp() throws Exception {
        messages = new GroupMessagesCollection();
    }

    @Test
    public void add_groupMessages() {
        String group = "Group";
        String group2 = "Group 2";

        Message simpleMessage = new SimpleMessage("message");
        Message rowMessage = new RowMessage("row message", 1);
        Message rowMessage2 = new RowMessage("another row message", 1);

        messages.addMessage(group, simpleMessage);
        messages.addMessage(group, rowMessage);
        messages.addMessage(group2, rowMessage2);

        GroupMessages groupMessages = messages.groupMessages(group);
        GroupMessages groupMessages2 = messages.groupMessages(group2);

        assertEquals("wrong groupMessages name", group, groupMessages.getName());
        assertEquals("wrong groupMessages name", group2, groupMessages2.getName());


        assertTrue("groupMessages missing message", groupMessages.messages().contains(simpleMessage.message()));
        assertTrue("groupMessages missing message", groupMessages.messages().contains(rowMessage.message()));
        assertFalse("groupMessages missing message", groupMessages.messages().contains(rowMessage2.message()));
        assertTrue("groupMessages missing message", groupMessages2.messages().contains(rowMessage2.message()));
    }

    @Test
    public void return_empty_GroupMessages_given_invalid_group() {
        GroupMessages emptyMessages = messages.groupMessages("doesn't exist");

        assertEquals("doesn't exist", emptyMessages.getName());
        assertEquals(0, emptyMessages.messages().size());
    }

}