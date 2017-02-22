package biocode.fims.renderers;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * @author rjewing
 */
public class MessagesGroupTest {
    private MessagesGroup messagesGroup;

    @Before
    public void setUp() throws Exception {
        messagesGroup = new MessagesGroup("Group");
    }

    @Test(expected = IllegalArgumentException.class)
    public void fails_fast_given_null_name() {
        new MessagesGroup(null);
    }

    @Test
    public void initialization() {
        assertEquals("Group", messagesGroup.getName());
        assertTrue(messagesGroup.messages().isEmpty());
    }

    @Test
    public void add_messages() {
        messagesGroup.add(new SimpleMessage("message 1"));
        messagesGroup.add(new SimpleMessage("message 2"));
        messagesGroup.add(new RowMessage("message 3", 1));

        assertEquals(3, messagesGroup.messages().size());
        assertTrue(messagesGroup.messages().contains("message 1"));
        assertTrue(messagesGroup.messages().contains("message 2"));
        assertTrue(messagesGroup.messages().contains("Row 1: message 3"));
    }

}