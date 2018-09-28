package biocode.fims.validation.rules;

import biocode.fims.config.models.Attribute;
import biocode.fims.records.Record;
import biocode.fims.records.RecordSet;
import biocode.fims.validation.messages.EntityMessages;
import biocode.fims.validation.messages.Message;
import org.springframework.util.Assert;

import java.util.*;
import java.util.stream.Collectors;

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
        for (Record r : recordSet.recordsToPersist()) {

            for (String uri : uris) {

                String value = r.get(uri);

                if (value.equals("")) {

                    if (level().equals(RuleLevel.ERROR)) r.setError();
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


    @Override
    public boolean mergeRule(Rule r) {
        if (!r.getClass().equals(this.getClass())) return false;

        MultiColumnRule rule = (MultiColumnRule) r;

        if (rule.level().equals(level())) {
            columns.addAll(rule.columns);
            networkRule = networkRule && rule.networkRule;
            return true;
        }
        return false;
    }

    @Override
    public boolean contains(Rule r) {
        if (!r.getClass().equals(this.getClass())) return false;

        MultiColumnRule rule = (MultiColumnRule) r;

        if (rule.level().equals(level())) {
            for (String c : rule.columns) {
                if (!columns.contains(c)) return false;
            }

            return true;
        }
        return false;
    }

    @Override
    public Rule toProjectRule(List<String> columns) {
        if (level().equals(RuleLevel.ERROR)) return this;

        LinkedHashSet<String> c = columns.stream().filter(columns::contains).collect(Collectors.toCollection(LinkedHashSet::new));

        return new RequiredValueRule(c, level());
    }
}
