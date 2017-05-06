package biocode.fims.validation.rules;

import biocode.fims.digester.Entity;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.LinkedList;
import java.util.List;

/**
 * @author rjewing
 */
abstract class MultiColumnRule implements Rule {
    @JsonProperty
    protected LinkedList<String> columns;
    protected boolean hasError;
    private RuleLevel level;

    public MultiColumnRule() {
        columns = new LinkedList<>();
    }

    @JsonIgnore
    @Override
    public void setColumn(String column) {
        throw new UnsupportedOperationException("MultiColumnRules do not implement the setColumn method. Use setColumns");
    }

    @JsonIgnore
    @Override
    public String column() {
        throw new UnsupportedOperationException("MultiColumnRules do not implement the column() method");
    }


    @JsonProperty
    public void setColumns(LinkedList<String> columns) {
        this.columns = columns;
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
    public boolean validConfiguration(List<String> messages) {
        return true;
    }

    @Override
    public boolean hasError() {
        return hasError;
    }

    protected void setError() {
        hasError = RuleLevel.ERROR == level;
    }

    protected LinkedList<String> getColumnUris(Entity entity) {
        LinkedList<String> uris = new LinkedList<>();

        for (String c : columns) {
            uris.add(entity.getAttributeUri(c));
        }

        return uris;
    }
}
