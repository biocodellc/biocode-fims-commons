package biocode.fims.validation.rules;

import biocode.fims.projectConfig.models.Entity;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * @author rjewing
 */
abstract class SingleColumnRule extends AbstractRule {
    @JsonProperty
    protected String column;

    SingleColumnRule() {
    }

    SingleColumnRule(String column, RuleLevel level) {
        super(level);
        this.column = column;
    }

    public String column() {
        return this.column;
    }

    @Override
    public boolean validConfiguration(List<String> messages, Entity entity) {
        if (StringUtils.isEmpty(column)) {
            messages.add("Invalid " + name() + " Rule configuration. Column must not be blank or null.");

            return false;
        }

        return entityHasAttribute(messages, entity, column);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof SingleColumnRule)) return false;
        if (!super.equals(o)) return false;

        SingleColumnRule that = (SingleColumnRule) o;

        return column != null ? column.equals(that.column) : that.column == null;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (column != null ? column.hashCode() : 0);
        return result;
    }
}
