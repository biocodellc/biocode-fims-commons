package biocode.fims.validation.rules;

import biocode.fims.digester.Entity;
import biocode.fims.models.records.Record;
import biocode.fims.models.records.RecordSet;
import biocode.fims.renderers.MessagesGroup;
import biocode.fims.renderers.SimpleMessage;
import org.apache.commons.lang.StringEscapeUtils;
import org.springframework.util.Assert;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Check a particular group of columns to see if the composite value combinations are unique.
 *
 * @author rjewing
 */
public class CompositeUniqueValueRule extends MultiColumnRule {
    private static final String NAME = "CompositeUniqueValue";
    private static final String GROUP_MESSAGE = "Unique value constraint did not pass";

    public CompositeUniqueValueRule() {
        super();
        this.messages = new MessagesGroup(GROUP_MESSAGE);
    }

    @Override
    public boolean run(RecordSet recordSet) {
        Assert.notNull(recordSet);

        LinkedList<String> uris = getColumnUris(recordSet.entity());

        Set<LinkedList<String>> set = new HashSet<>();
        List<LinkedList<String>> duplicateValues = new ArrayList<>();

        for (Record r : recordSet.records()) {
            LinkedList<String> composite = new LinkedList<>();

            for (String uri : uris) {
                composite.add(r.get(uri));
            }

            if (!set.add(composite)) {
                duplicateValues.add(composite);
            }

        }

        if (duplicateValues.size() == 0) {
            return true;
        }

        setMessages(duplicateValues);
        return false;
    }

    private LinkedList<String> getColumnUris(Entity entity) {
        LinkedList<String> uris = new LinkedList<>();

        for (String c : columns) {
            uris.add(entity.getAttributeUri(c));
        }

        return uris;
    }

    private void setMessages(List<LinkedList<String>> invalidValues) {
        List<String> compositeValues = new ArrayList<>();

        for (LinkedList<String> values : invalidValues) {
            compositeValues.add(
                    String.join("\", \"", values)
            );
        }

        messages.add(new SimpleMessage(
                "(\"" + String.join("\", \"", columns) + "\") is defined as a composite unique key, but" +
                        " some value combinations were used more than once: (\"" + String.join("\"), (\"", compositeValues) + "\")"
        ));
    }

    @Override
    public String name() {
        return NAME;
    }

}
