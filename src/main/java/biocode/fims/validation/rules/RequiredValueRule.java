package biocode.fims.validation.rules;

import biocode.fims.digester.Attribute;
import biocode.fims.models.records.Record;
import biocode.fims.models.records.RecordSet;
import biocode.fims.validation.messages.EntityMessages;
import biocode.fims.validation.messages.Message;
import org.springframework.util.Assert;

import java.util.*;

/**
 * For each column in Columns, check that there are no missing values
 *
 * @author rjewing
 */
public class RequiredValueRule extends MultiColumnRule {
    private static final String NAME = "RequiredValue";
    private static final String GROUP_MESSAGE = "Missing column(s)";

    // needed for RuleTypeIdResolver to dynamically instantiate Rule implementation
    private RequiredValueRule() {
    }

    public RequiredValueRule(LinkedHashSet<String> columns, RuleLevel level) {
        super(columns, level);
    }

    public RequiredValueRule(LinkedHashSet<String> columns) {
        this(columns, RuleLevel.WARNING);
    }

    @Override
    public boolean run(RecordSet recordSet, EntityMessages messages) {
        Assert.notNull(recordSet);

        if (!validConfiguration(recordSet, messages)) {
            return false;
        }

        List<String> uris = getColumnUris(recordSet.entity());

        List<String> columnsMissingValues = new ArrayList<>();

        List<String> urisToRemove = new ArrayList<>();
        for (Record r : recordSet.records()) {

            for (String uri : uris) {

                String value = r.get(uri);

                if (value.equals("")) {

                    columnsMissingValues.add(
                            getColumnFromUri(uri, recordSet.entity().getAttributes())
                    );
                    urisToRemove.add(uri);
                }
            }

            if (urisToRemove.size() > 0) {
                uris.removeAll(urisToRemove);
                urisToRemove.clear();
            }

            if (uris.isEmpty()) {
                break;
            }
        }

        if (columnsMissingValues.size() == 0) {
            return true;
        }

        setMessages(messages, columnsMissingValues);
        setError();
        return false;
    }

    private String getColumnFromUri(String uri, List<Attribute> attributes) {
        for (Attribute a : attributes) {
            if (a.getUri().equals(uri)) {
                return a.getColumn();
            }
        }

        return null;
    }

    private void setMessages(EntityMessages messages, List<String> columnsMissingValues) {
        String msgLevel = "desirable";

        if (level() == RuleLevel.ERROR) {
            msgLevel = "mandatory";
        }

        for (String c : columnsMissingValues) {
            messages.addMessage(
                    GROUP_MESSAGE,
                    new Message(
                            "\"" + c + "\" has a missing cell value"
                    ),
                    level()
            );
        }
    }

    @Override
    public String name() {
        return NAME;
    }

}
