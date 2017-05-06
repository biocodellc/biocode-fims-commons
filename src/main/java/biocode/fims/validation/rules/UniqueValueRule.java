package biocode.fims.validation.rules;

import biocode.fims.models.records.RecordSet;
import biocode.fims.renderers.MessagesGroup;
import biocode.fims.renderers.SimpleMessage;
import org.springframework.util.Assert;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Check a particular column to see if all the values are unique.
 * This rule is strongly encouraged for at least one column in the spreadsheet
 * <p>
 * NOTE: that NULL values are not counted in this rule, so this rule, by itself does not
 * enforce a primary key... it must be combined with a rule requiring some column value
 *
 * @author rjewing
 */
public class UniqueValueRule implements Rule {
    private static final String NAME = "uniqueValue";
    private static final String GROUP_MESSAGE = "Unique value constraint did not pass";

    private String column;
    private RuleLevel level;
    private MessagesGroup messages;

    public UniqueValueRule() {
        this.messages = new MessagesGroup(GROUP_MESSAGE);
    }

    @Override
    public boolean run(RecordSet recordSet) {
        Assert.notNull(recordSet);

        String uri = recordSet.entity().getAttributeUri(column);

        Set<String> set = new HashSet<>();

        List<String> duplicateValues = recordSet.records()
                .stream()
                .map(r -> r.get(uri))
                .filter(v -> !v.equals(""))
                .filter(v -> !set.add(v))
                .collect(Collectors.toList());

        if (duplicateValues.size() == 0) {
            return true;
        }

        setMessages(duplicateValues);
        return false;
    }

    private void setMessages(List<String> invalidValues) {
        messages.add(new SimpleMessage(
                "\"" + column + "\" column is defined as unique but some values used more than once: \"" + String.join("\", \"", invalidValues) + "\""
        ));
    }

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public void setColumn(String column) {
        this.column = column;
    }

    @Override
    public void setLevel(RuleLevel level) {
        this.level = level;
    }

    @Override
    public RuleLevel level() {
        return level;
    }

    @Override
    public MessagesGroup messages() {
        return messages;
    }

}
