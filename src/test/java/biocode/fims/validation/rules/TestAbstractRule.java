package biocode.fims.validation.rules;

import biocode.fims.renderers.MessagesGroup;

/**
 * @author rjewing
 */
abstract class TestAbstractRule implements Rule {
    protected String column;
    protected MessagesGroup messages;
    private RuleLevel level;

    TestAbstractRule() {
        messages = new MessagesGroup("MultiColumnRule");
    }

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
    public MessagesGroup messages() {
        return messages;
    }
}
