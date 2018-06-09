package biocode.fims.validation.rules;

import biocode.fims.models.records.Record;
import biocode.fims.models.records.RecordSet;
import biocode.fims.validation.messages.EntityMessages;
import biocode.fims.validation.messages.Message;
import org.springframework.util.Assert;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Check a particular column to see if all the values are unique.
 * This rule is strongly encouraged for at least one column in the spreadsheet
 * <p>
 * NOTE: that NULL values are not counted in this rule, so this rule, by itself does not
 * enforce a primary key... it must be combined with a rule requiring some column value
 *
 * @author rjewing
 */
public class UniqueValueRule extends SingleColumnRule {
    private static final String NAME = "UniqueValue";
    private static final String GROUP_MESSAGE = "Unique value constraint did not pass";

    // needed for RuleTypeIdResolver to dynamically instantiate Rule implementation
    private UniqueValueRule() {
    }

    public UniqueValueRule(String column, RuleLevel level) {
        super(column, level);
    }

    public UniqueValueRule(String column) {
        this(column, RuleLevel.WARNING);
    }

    @Override
    public boolean run(RecordSet recordSet, EntityMessages messages) {
        Assert.notNull(recordSet);

        if (!validConfiguration(recordSet, messages)) {
            return false;
        }

        String uri = recordSet.entity().getAttributeUri(column);

        Set<String> set = new HashSet<>();
        List<String> duplicateValues = new ArrayList<>();

        for (Record r : recordSet.recordsToPersist()) {

            String value = r.get(uri);

            if (!value.equals("") && !set.add(value)) {
                duplicateValues.add(value);
            }

        }

        if (duplicateValues.size() == 0) {
            return true;
        }

        setMessages(duplicateValues, messages);
        setError();
        return false;
    }

    private void setMessages(List<String> invalidValues, EntityMessages messages) {
        messages.addMessage(
                GROUP_MESSAGE,
                new Message(
                        "\"" + column + "\" column is defined as unique but some values used more than once: \"" + String.join("\", \"", invalidValues) + "\""
                ),
                level()
        );
    }

    @Override
    public String name() {
        return NAME;
    }

}
