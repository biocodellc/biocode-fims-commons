package biocode.fims.validation.rules;

import biocode.fims.renderers.MessagesGroup;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;
import java.util.List;

/**
 * @author rjewing
 */
abstract class MultiColumnRule implements Rule {
    @JsonProperty
    protected List<String> columns;
    protected MessagesGroup messages;
    private RuleLevel level;

    public MultiColumnRule() {
        columns = new ArrayList<>();
        messages = new MessagesGroup("MultiColumnRule");
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
    public void setColumns(List<String> columns) {
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
    public MessagesGroup messages() {
        return messages;
    }
}
