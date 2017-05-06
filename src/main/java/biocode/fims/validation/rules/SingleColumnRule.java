package biocode.fims.validation.rules;

import biocode.fims.digester.Entity;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * @author rjewing
 */
abstract class SingleColumnRule extends AbstractRule {
    @JsonProperty
    protected String column;

    SingleColumnRule() {}

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

}
