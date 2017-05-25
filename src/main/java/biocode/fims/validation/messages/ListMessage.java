package biocode.fims.validation.messages;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.util.Assert;

import java.util.List;

/**
 * @author rjewing
 */
public class ListMessage extends Message {
    private final List<String> list;

    public ListMessage(List<String> list, String msg) {
        super(msg);
        Assert.notNull(list);
        this.list = list;
    }

    @JsonProperty("list")
    public List<String> list() {
        return list;
    }

    @Override
    public String toString() {
        return "ListMessage{" +
                "list=" + list +
                ", message='" + message() + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ListMessage)) return false;
        if (!super.equals(o)) return false;

        ListMessage that = (ListMessage) o;

        return list.equals(that.list);
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + list.hashCode();
        return result;
    }

}
