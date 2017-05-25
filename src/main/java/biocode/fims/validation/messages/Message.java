package biocode.fims.validation.messages;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * @author rjewing
 */
public class Message {
    private final String message;

    public Message(String msg) {
        message = msg;
    }

    @JsonProperty("message")
    public String message() {
        return message;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Message)) return false;

        Message that = (Message) o;

        return message.equals(that.message);
    }

    @Override
    public int hashCode() {
        return message.hashCode();
    }
}
