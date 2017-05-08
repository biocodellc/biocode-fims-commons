package biocode.fims.validation.rules;

import biocode.fims.digester.Entity;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

/**
 * @author rjewing
 */
abstract class MultiColumnRule extends AbstractRule {
    @JsonProperty
    protected LinkedHashSet<String> columns;

    MultiColumnRule() {}

    MultiColumnRule(LinkedHashSet<String> columns, RuleLevel level) {
        super(level);
        this.columns = columns;
    }

    public void addColumn(String column) {
        columns.add(column);
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof MultiColumnRule)) return false;
        if (!super.equals(o)) return false;

        MultiColumnRule that = (MultiColumnRule) o;

        return columns != null ? columns.equals(that.columns) : that.columns == null;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (columns != null ? columns.hashCode() : 0);
        return result;
    }
}
