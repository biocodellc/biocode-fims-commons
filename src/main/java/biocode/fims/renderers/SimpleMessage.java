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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof SimpleMessage)) return false;

        SimpleMessage that = (SimpleMessage) o;

        return message.equals(that.message);
    }

    @Override
    public int hashCode() {
        return message.hashCode();
    }
}
