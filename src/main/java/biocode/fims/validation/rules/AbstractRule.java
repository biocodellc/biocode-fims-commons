package biocode.fims.validation.rules;

/**
 * @author rjewing
 */
abstract class AbstractRule implements Rule {
    protected String column;
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

}
