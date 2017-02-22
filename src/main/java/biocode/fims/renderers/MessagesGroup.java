package biocode.fims.renderers;


import org.springframework.util.Assert;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author rjewing
 */
public class MessagesGroup {
    private final String name;
    private List<Message> messages;

    public MessagesGroup(String name) {
        Assert.notNull(name);

        this.name = name;
        this.messages = new ArrayList<>();
    }

    public String getName() {
        return name;
    }

    public void add(Message msg) {
        messages.add(msg);
    }

    public List<String> messages() {
        return messages.stream()
                .map(Message::message)
                .collect(Collectors.toList());
    }
}
