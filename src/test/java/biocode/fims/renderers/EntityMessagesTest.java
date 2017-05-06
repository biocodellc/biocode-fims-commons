package biocode.fims.renderers;

import biocode.fims.validation.rules.RuleLevel;
import org.junit.Before;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.*;

/**
 * @author rjewing
 */
public class EntityMessagesTest {

    private EntityMessages messages;

    @Before
    public void setUp() throws Exception {
        messages = new EntityMessages("Samples", "Samples");
    }

    @Test
    public void initialization() {
        assertEquals("Samples", messages.sheetName());
        assertTrue(messages.errorMessages().isEmpty());
        assertTrue(messages.warningMessages().isEmpty());
    }

    @Test
    public void add_warning_message() {
        String group = "Group";
        String group2 = "Group 2";
        Message simpleMessage = new SimpleMessage("warning");
        Message simpleMessage1 = new SimpleMessage("warning 1");
        Message simpleMessage2 = new SimpleMessage("warning 2");

        messages.addWarningMessage(group, simpleMessage);
        messages.addMessage(group, simpleMessage1, RuleLevel.WARNING);
        messages.addWarningMessage(group2, simpleMessage2);

        List<MessagesGroup> warningMessages = messages.warningMessages();

        assertEquals(2, warningMessages.size());
    }

    @Test
    public void add_error_and_warning_messages() {
        String group = "Group";
        String group2 = "Group 2";
        Message simpleMessage = new SimpleMessage("error");
        Message simpleMessage1 = new SimpleMessage("warning 1");
        Message simpleMessage2 = new SimpleMessage("warning 2");
        Message simpleMessage3 = new SimpleMessage("warning 3");

        messages.addErrorMessage(group, simpleMessage);
        messages.addWarningMessage(group, simpleMessage1);
        messages.addWarningMessage(group2, simpleMessage2);
        messages.addMessage(group2, simpleMessage3, RuleLevel.ERROR);

        List<MessagesGroup> warningMessages = messages.warningMessages();
        List<MessagesGroup> errorMessages = messages.errorMessages();

        assertEquals(2, warningMessages.size());
        assertEquals(2, errorMessages.size());
    }

}