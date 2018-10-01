package biocode.fims.validation.rules;

import biocode.fims.config.models.Entity;
import biocode.fims.records.Record;
import biocode.fims.records.RecordSet;
import biocode.fims.validation.messages.EntityMessages;
import biocode.fims.validation.messages.Message;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * If otherColumn has data, check that column also has data
 *
 * @author rjewing
 */
public class RequireValueIfOtherColumnRule extends SingleColumnRule {
    private static final String NAME = "RequireValueIfOtherColumn";
    private static final String GROUP_MESSAGE = "Dependent column value check";
    @JsonProperty
    private String otherColumn;

    // needed for RuleTypeIdResolver to dynamically instantiate Rule implementation
    private RequireValueIfOtherColumnRule() {
    }

    public RequireValueIfOtherColumnRule(String column, String otherColumn, RuleLevel level) {
        super(column, level);
        this.otherColumn = otherColumn;
    }

    public RequireValueIfOtherColumnRule(String column, String otherColumn) {
        this(column, otherColumn, RuleLevel.WARNING);
    }

    @Override
    public boolean run(RecordSet recordSet, EntityMessages messages) {
        Assert.notNull(recordSet);
        boolean valid = true;

        if (!validConfiguration(recordSet, messages)) {
            return false;
        }

        String uri = recordSet.entity().getAttributeUri(column);
        String otherUri = recordSet.entity().getAttributeUri(otherColumn);

        for (Record r : recordSet.recordsToPersist()) {

            String value = r.get(uri);
            String otherValue = r.get(otherUri);

            if (!otherValue.equals("") && value.equals("")) {
                valid = false;
                if (level().equals(RuleLevel.ERROR)) r.setError();
                messages.addMessage(
                        GROUP_MESSAGE,
                        new Message(
                                "\"" + otherColumn + "\" has value \"" + otherValue + "\", but associated column \""
                                        + column + "\" has no value"),
                        level()
                );
            }
        }
        if (!valid) {
            setError();
        }

        return valid;
    }

    @Override
    public boolean validConfiguration(List<String> messages, Entity entity) {
        boolean valid = super.validConfiguration(messages, entity);

        if (StringUtils.isEmpty(otherColumn)) {
            messages.add("Invalid " + name() + " Rule configuration. otherColumn must not be blank or null.");

            return false;
        }

        if (!valid) {
            return false;
        }

        return entityHasAttribute(messages, entity, otherColumn);
    }

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public Rule toProjectRule(List<String> columns) {
        if (level().equals(RuleLevel.ERROR)) {
            if (columns.contains(column) || columns.contains(otherColumn)) return this;
        } else {
            if (columns.contains(column) && columns.contains(otherColumn)) return this;
        }

        return null;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof RequireValueIfOtherColumnRule)) return false;
        if (!super.equals(o)) return false;

        RequireValueIfOtherColumnRule that = (RequireValueIfOtherColumnRule) o;

        return otherColumn != null ? otherColumn.equals(that.otherColumn) : that.otherColumn == null;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (otherColumn != null ? otherColumn.hashCode() : 0);
        return result;
    }
}
