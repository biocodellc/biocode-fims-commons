package biocode.fims.validation.rules;

import java.util.List;

/**
 * @author rjewing
 */
abstract class AbstractRule implements Rule {
    protected String column;
    protected boolean hasError = false;
    private RuleLevel level;

    @Override
    public String column() {
        return this.column;
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
}
