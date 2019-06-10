package biocode.fims.validation.rules;

import biocode.fims.records.Record;
import biocode.fims.records.RecordSet;
import biocode.fims.validation.messages.EntityMessages;
import biocode.fims.validation.messages.Message;
import org.apache.commons.collections4.keyvalue.MultiKey;
import org.springframework.util.Assert;

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

        String uploadingExpeditionCode = recordSet.expeditionCode();
        set.addAll(
                recordSet.records().stream()
                        .filter(r -> Objects.equals(r.expeditionCode(), uploadingExpeditionCode) && !r.persist())
                        .map(r -> buildCompositeValue(uris, r))
                        .collect(Collectors.toList())
        );

        for (Record r : recordSet.recordsToPersist()) {
            LinkedList<String> composite = buildCompositeValue(uris, r);

            if (composite.size() > 0 && !set.add(composite)) {
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

    private LinkedList<String> buildCompositeValue(LinkedList<String> uris, Record r) {
        LinkedList<String> composite = new LinkedList<>();

        boolean hasAValue = false;
        for (String uri : uris) {
            String val = r.get(uri);
            composite.add(val);
            if (!val.equals("")) hasAValue = true;
        }

        return hasAValue ? composite : new LinkedList<String>();
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
                .filter(this.columns::contains)
                .collect(Collectors.toCollection(LinkedHashSet::new));

        if (c.size() == 1) return new UniqueValueRule(c.iterator().next(), false, level());
        if (c.size() > 0) return new CompositeUniqueValueRule(c, level());

        return null;
    }
}
