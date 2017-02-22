package biocode.fims.renderers;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * @author rjewing
 */
public class MessagesGroupCollectionTest {

    private MessagesGroupCollection messages;

    @Before
    public void setUp() throws Exception {
        messages = new MessagesGroupCollection();
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

        MessagesGroup messagesGroup = messages.groupMessages(group);
        MessagesGroup messagesGroup2 = messages.groupMessages(group2);

        assertEquals("wrong messagesGroup name", group, messagesGroup.getName());
        assertEquals("wrong messagesGroup name", group2, messagesGroup2.getName());


        assertTrue("messagesGroup missing message", messagesGroup.messages().contains(simpleMessage.message()));
        assertTrue("messagesGroup missing message", messagesGroup.messages().contains(rowMessage.message()));
        assertFalse("messagesGroup missing message", messagesGroup.messages().contains(rowMessage2.message()));
        assertTrue("messagesGroup missing message", messagesGroup2.messages().contains(rowMessage2.message()));
    }

    @Test
    public void return_empty_GroupMessages_given_invalid_group() {
        MessagesGroup emptyMessages = messages.groupMessages("doesn't exist");

        assertEquals("doesn't exist", emptyMessages.getName());
        assertEquals(0, emptyMessages.messages().size());
    }

}