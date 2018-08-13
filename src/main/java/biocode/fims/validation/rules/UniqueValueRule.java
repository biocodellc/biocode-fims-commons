package biocode.fims.validation.rules;

import biocode.fims.models.records.Record;
import biocode.fims.models.records.RecordSet;
import biocode.fims.validation.messages.EntityMessages;
import biocode.fims.validation.messages.Message;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.util.Assert;

import java.util.ArrayList;
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
public class UniqueValueRule extends SingleColumnRule {
    private static final String NAME = "UniqueValue";
    private static final String GROUP_MESSAGE = "Unique value constraint did not pass";
    @JsonProperty
    private boolean uniqueAcrossProject = false;

    // needed for RuleTypeIdResolver to dynamically instantiate Rule implementation
    private UniqueValueRule() {
    }

    public UniqueValueRule(String column, boolean uniqueAcrossProject, RuleLevel level) {
        super(column, level);
        this.uniqueAcrossProject = uniqueAcrossProject;
    }

    public UniqueValueRule(String column, boolean uniqueAcrossProject) {
        this(column, uniqueAcrossProject, RuleLevel.WARNING);
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

        List<Record> recordsToCheck;
        List<Record> recordsToPersist = recordSet.recordsToPersist();

        if (recordsToPersist.size() == 0) {
            recordsToCheck = new ArrayList<>();
        } else if (uniqueAcrossProject) {
            recordsToCheck = recordSet.records();
        } else {
            // can only upload to a single expedition, so we can look at the first record to persist
            String uploadingExpeditionCode = recordSet.expeditionCode();

            recordsToCheck = recordSet.records().stream()
                    .filter(r -> r.expeditionCode().equals(uploadingExpeditionCode))
                    .collect(Collectors.toList());
        }

        for (Record r : recordsToCheck) {

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

    public boolean uniqueAcrossProject() {
        return uniqueAcrossProject;
    }

    private void setMessages(List<String> invalidValues, EntityMessages messages) {
        String msg = "\"" + column + "\" column is defined as unique ";
        if (uniqueAcrossProject) msg += "across the entire project ";
        msg += "but some values used more than once: \"" + String.join("\", \"", invalidValues) + "\"";
        messages.addMessage(
                GROUP_MESSAGE,
                new Message(msg),
                level()
        );
    }

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof UniqueValueRule)) return false;
        if (!super.equals(o)) return false;

        UniqueValueRule that = (UniqueValueRule) o;

        return uniqueAcrossProject == that.uniqueAcrossProject;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (uniqueAcrossProject ? 1 : 0);
        return result;
    }
}
