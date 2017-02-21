package biocode.fims.renderers;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * @author rjewing
 */
public class SimpleMessageTest {
    private SimpleMessage msg;

    @Before
    public void setUp() throws Exception {
        msg = new SimpleMessage("message");
    }

    @Test
    public void returns_given_message() {
        assertEquals("message", msg.message());
    }

    @Test
    public void test_value_object() {
        SimpleMessage msg2 = new SimpleMessage("message");

        assertEquals(msg, msg2);
    }

}