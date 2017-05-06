package biocode.fims.validation.rules;

import biocode.fims.models.records.RecordSet;

/**
 * @author rjewing
 */
public class TestRule extends AbstractRule {
    @Override
    public String name() {
        return "TestRule";
    }

    @Override
    public boolean run(RecordSet recordSet) {
        return false;
    }
}
