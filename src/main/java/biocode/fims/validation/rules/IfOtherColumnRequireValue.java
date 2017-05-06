package biocode.fims.validation.rules;

import biocode.fims.digester.Entity;
import biocode.fims.models.records.Record;
import biocode.fims.models.records.RecordSet;
import biocode.fims.renderers.EntityMessages;
import biocode.fims.renderers.SimpleMessage;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * If otherColumn has data, check that column also has data
 *
 * @author rjewing
 */
public class IfOtherColumnRequireValue extends SingleColumnRule {
    private static final String NAME = "IfOtherColumnRequireValue";
    private static final String GROUP_MESSAGE = "Dependent column value check";
    private String otherColumn;

    // needed for RuleTypeIdResolver to dynamically instantiate Rule implementation
    IfOtherColumnRequireValue() {
    }

    public IfOtherColumnRequireValue(String column, String otherColumn, RuleLevel level) {
        super(column, level);
        this.otherColumn = otherColumn;
    }

    public IfOtherColumnRequireValue(String column, String otherColumn) {
        this(column, otherColumn, RuleLevel.WARNING);
    }

    @Override
    public boolean run(RecordSet recordSet, EntityMessages messages) {
        Assert.notNull(recordSet);
        boolean valid = true;

        if (!validConfiguration(recordSet, messages)) {
            return false;
        }

        String uri = recordSet.entity().getAttributeUri(column);
        String otherUri = recordSet.entity().getAttributeUri(otherColumn);

        for (Record r : recordSet.records()) {

            String value = r.get(uri);
            String otherValue = r.get(otherUri);

            if (!otherValue.equals("") && value.equals("")) {
                valid = false;
                messages.addMessage(
                        GROUP_MESSAGE,
                        new SimpleMessage(
                                "\"" + otherColumn + "\" has value \"" + otherValue + "\", but associated column \""
                                        + column + "\" has no value"),
                        level()
                );
            }
        }
        if (!valid) {
            setError();
        }

        return valid;
    }

    @Override
    public boolean validConfiguration(List<String> messages, Entity entity) {
        boolean valid = super.validConfiguration(messages, entity);

        if (StringUtils.isEmpty(otherColumn)) {
            messages.add("Invalid " + name() + " Rule configuration. otherColumn must not be blank or null.");

            return false;
        }

        if (!valid) {
            return false;
        }

        return entityHasAttribute(messages, entity, otherColumn);
    }

    @Override
    public String name() {
        return NAME;
    }

}
