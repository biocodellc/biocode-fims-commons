package biocode.fims.validation.rules;

import biocode.fims.models.records.Record;
import biocode.fims.models.records.RecordSet;
import biocode.fims.renderers.EntityMessages;
import biocode.fims.renderers.SimpleMessage;
import org.springframework.util.Assert;

import java.util.*;

/**
 * Check a particular group of columns to see if the composite value combinations are unique.
 *
 * @author rjewing
 */
public class CompositeUniqueValueRule extends MultiColumnRule {
    private static final String NAME = "CompositeUniqueValue";
    private static final String GROUP_MESSAGE = "Unique value constraint did not pass";

    // needed for RuleTypeIdResolver to dynamically instantiate Rule implementation
    CompositeUniqueValueRule() {}

    public CompositeUniqueValueRule(List<String> columns) {
        super(columns, RuleLevel.WARNING);
    }

    public CompositeUniqueValueRule(List<String> columns, RuleLevel level) {
        super(columns, level);
    }

    @Override
    public boolean run(RecordSet recordSet, EntityMessages messages) {
        Assert.notNull(recordSet);

        if (!validConfiguration(recordSet, messages)) {
            return false;
        }

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

        setMessages(messages, duplicateValues);
        setError();
        return false;
    }

    private void setMessages(EntityMessages messages, List<LinkedList<String>> invalidValues) {
        List<String> compositeValues = new ArrayList<>();

        for (LinkedList<String> values : invalidValues) {
            compositeValues.add(
                    String.join("\", \"", values)
            );
        }

        messages.addMessage(
                GROUP_MESSAGE,
                new SimpleMessage(
                        "(\"" + String.join("\", \"", columns) + "\") is defined as a composite unique key, but" +
                                " some value combinations were used more than once: (\"" + String.join("\"), (\"", compositeValues) + "\")"),
                level()
        );
    }

    @Override
    public String name() {
        return NAME;
    }
}
