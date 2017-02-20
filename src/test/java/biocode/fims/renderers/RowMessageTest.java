package biocode.fims.renderers;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * @author rjewing
 */
public class RowMessageTest {

    @Test
    public void check_message() {
        Message msg = new RowMessage("test message", 1);
        assertEquals("Row 1: test message", msg.message());
    }

}