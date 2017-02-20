package biocode.fims.renderers;

/**
 * @author rjewing
 */
public class SimpleMessage implements Message {
    private final String message;

    public SimpleMessage(String msg) {
        message = msg;
    }

    @Override
    public String message() {
        return message;
    }
}
