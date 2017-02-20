package biocode.fims.renderers;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * @author rjewing
 */
public class GroupMessagesTest {
    private GroupMessages groupMessages;

    @Before
    public void setUp() throws Exception {
        groupMessages = new GroupMessages("Group");
    }

    @Test(expected = IllegalArgumentException.class)
    public void fails_fast_given_null_name() {
        new GroupMessages(null);
    }

    @Test
    public void initialization() {
        assertEquals("Group", groupMessages.getName());
        assertTrue(groupMessages.messages().isEmpty());
    }

    @Test
    public void add_messages() {
        groupMessages.add(new SimpleMessage("message 1"));
        groupMessages.add(new SimpleMessage("message 2"));
        groupMessages.add(new RowMessage("message 3", 1));

        assertEquals(3, groupMessages.messages().size());
        assertTrue(groupMessages.messages().contains("message 1"));
        assertTrue(groupMessages.messages().contains("message 2"));
        assertTrue(groupMessages.messages().contains("Row 1: message 3"));
    }

}