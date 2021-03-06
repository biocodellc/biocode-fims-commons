package biocode.fims.validation.messages;


import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.util.Assert;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @author rjewing
 */
public class MessagesGroup {
    private final String name;
    private Set<Message> messages;

    public MessagesGroup(String name) {
        Assert.notNull(name);

        this.name = name;
        this.messages = new HashSet<>();
    }

    @JsonProperty("groupMessage")
    public String getName() {
        return name;
    }

    public void add(Message msg) {
        messages.add(msg);
    }

    @JsonProperty("messages")
    public List<Message> messages() {
        return new ArrayList<>(messages);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof MessagesGroup)) return false;

        MessagesGroup that = (MessagesGroup) o;

        if (!getName().equals(that.getName())) return false;
        return messages.equals(that.messages);
    }

    @Override
    public int hashCode() {
        int result = getName().hashCode();
        result = 31 * result + messages.hashCode();
        return result;
    }

    @Override
    public String toString() {
        StringBuilder s = new StringBuilder("MessagesGroup{" +
                "GroupName='" + name + '\'' +
                ", messages=[");

        for (Message m : messages) {
            s.append(m.message()).append(",");
        }

        s.append("]}");
        return s.toString();
    }
}
