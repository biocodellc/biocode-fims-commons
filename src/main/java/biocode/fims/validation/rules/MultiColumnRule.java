package biocode.fims.validation.rules;

import biocode.fims.digester.Entity;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.LinkedList;
import java.util.List;

/**
 * @author rjewing
 */
abstract class MultiColumnRule extends AbstractRule {
    @JsonProperty
    protected List<String> columns;

    MultiColumnRule() {}

    MultiColumnRule(List<String> columns, RuleLevel level) {
        super(level);
        this.columns = columns;
    }

    @Override
    public boolean validConfiguration(List<String> messages, Entity entity) {
        if (columns.isEmpty()) {
            messages.add("Invalid " + name() + " Rule configuration. columns must not be empty.");

            return false;
        }

        return checkColumnsExist(messages, entity);
    }

    private boolean checkColumnsExist(List<String> messages, Entity entity) {
        boolean valid = true;

        for (String c: columns) {
            if (!entityHasAttribute(messages, entity, c)) {
                valid = false;
            }
        }

        return valid;
    }

    LinkedList<String> getColumnUris(Entity entity) {
        LinkedList<String> uris = new LinkedList<>();

        for (String c : columns) {
            uris.add(entity.getAttributeUri(c));
        }

        return uris;
    }
}
