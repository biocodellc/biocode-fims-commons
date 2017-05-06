package biocode.fims.validation.rules;

import biocode.fims.models.records.RecordSet;
import biocode.fims.renderers.EntityMessages;

/**
 * @author rjewing
 */
public class TestRule extends AbstractRule {
    @Override
    public String name() {
        return "TestRule";
    }

    @Override
    public boolean run(RecordSet recordSet, EntityMessages messages) {
        return false;
    }
}
