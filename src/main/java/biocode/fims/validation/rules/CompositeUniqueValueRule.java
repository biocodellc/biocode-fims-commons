package biocode.fims.validation.rules;

import biocode.fims.records.Record;
import biocode.fims.records.RecordSet;
import biocode.fims.validation.messages.EntityMessages;
import biocode.fims.validation.messages.Message;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Check a particular group of columns to see if the composite value combinations are unique,
 * if and only if at least 1 column in the group has a value. If all columns are empty, then
 * this rule will pass.
 *
 * @author rjewing
 */
public class CompositeUniqueValueRule extends MultiColumnRule {
    private static final String NAME = "CompositeUniqueValue";
    private static final String GROUP_MESSAGE = "Unique value constraint did not pass";

    // needed for RuleTypeIdResolver to dynamically instantiate Rule implementation
    private CompositeUniqueValueRule() {
    }

    public CompositeUniqueValueRule(LinkedHashSet<String> columns, RuleLevel level) {
        super(columns, level);
    }

    public CompositeUniqueValueRule(LinkedHashSet<String> columns) {
        this(columns, RuleLevel.WARNING);
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

        for (Record r : recordSet.recordsToPersist()) {
            LinkedList<String> composite = new LinkedList<>();

            boolean hasAValue = false;
            for (String uri : uris) {
                String val = r.get(uri);
                composite.add(val);
                if (!val.equals("")) hasAValue = true;
            }

            if (hasAValue && !set.add(composite)) {
                duplicateValues.add(composite);
                if (level().equals(RuleLevel.ERROR)) r.setError();
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
                new Message(
                        "(\"" + String.join("\", \"", columns) + "\") is defined as a composite unique key, but" +
                                " some value combinations were used more than once: (\"" + String.join("\"), (\"", compositeValues) + "\")"),
                level()
        );
    }

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public Rule toProjectRule(List<String> columns) {
        LinkedHashSet<String> c = columns.stream()
                .filter(columns::contains)
                .collect(Collectors.toCollection(LinkedHashSet::new));

        if (c.size() == 1) return new UniqueValueRule(c.iterator().next(), false, level());
        if (c.size() > 0) return new CompositeUniqueValueRule(c, level());

        return null;
    }
}
