package biocode.fims.validation.rules;

import biocode.fims.digester.Entity;
import biocode.fims.models.records.Record;
import biocode.fims.models.records.RecordSet;
import biocode.fims.renderers.EntityMessages;
import biocode.fims.renderers.SimpleMessage;
import org.springframework.util.Assert;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * check that at least 1 column in the columns list has a value
 *
 * @author rjewing
 */
public class RequiredValueInGroupRule extends MultiColumnRule {
    private static final String NAME = "RequiredValueInGroup";
    private static final String GROUP_MESSAGE = "Missing column from group";

    private String uniqueKeyColumn;

    // needed for RuleTypeIdResolver to dynamically instantiate Rule implementation
    RequiredValueInGroupRule() {
    }

    public RequiredValueInGroupRule(LinkedHashSet<String> columns) {
        super(columns, RuleLevel.WARNING);
    }

    public RequiredValueInGroupRule(LinkedHashSet<String> columns, RuleLevel level) {
        super(columns, level);
    }

    @Override
    public boolean run(RecordSet recordSet, EntityMessages messages) {
        Assert.notNull(recordSet);

        if (!validConfiguration(recordSet, messages)) {
            return false;
        }

        Entity e = recordSet.entity();
        uniqueKeyColumn = e.getUniqueKey();
        String uniqueKeyUri = e.getUniqueKeyURI();
        List<String> uris = getColumnUris(e);

        List<String> columnsMissingValues = new ArrayList<>();

        for (Record r : recordSet.records()) {

            boolean valid = false;
            for (String uri : uris) {

                String value = r.get(uri);

                if (!value.equals("")) {
                    valid = true;
                    break;
                }
            }

            if (!valid) {
                String uniqueKey = r.get(uniqueKeyUri);
                columnsMissingValues.add(uniqueKey);
            }

        }

        if (columnsMissingValues.size() == 0) {
            return true;
        }

        setMessages(messages, columnsMissingValues);
        setError();
        return false;
    }

    private void setMessages(EntityMessages messages, List<String> columnsMissingValues) {

        for (String key : columnsMissingValues) {
            messages.addMessage(
                    GROUP_MESSAGE,
                    new SimpleMessage(
                            "row with " + uniqueKeyColumn + "=" + key + " must have a value in at least 1 of the " +
                                    "columns: [\"" + String.join("\",\"", columns) + "\"]"),
                    level()
            );
        }
    }

    @Override
    public String name() {
        return NAME;
    }

}
