package biocode.fims.validation.rules;

import biocode.fims.digester.Entity;
import biocode.fims.models.records.Record;
import biocode.fims.models.records.RecordSet;
import biocode.fims.validation.messages.EntityMessages;
import biocode.fims.validation.messages.Message;
import org.springframework.util.Assert;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

/**
 * Checks that the each value specified in the parent id column exists in the parent RecordSet
 *
 * @author rjewing
 */
public class ValidParentIdentifiersRule extends AbstractRule {
    private static final String NAME = "ValidParentIdentifiers";
    private static final String GROUP_MESSAGE = "Invalid parent identifier(s)";

    public ValidParentIdentifiersRule() {
        super(RuleLevel.ERROR);
    }

    @Override
    public boolean run(RecordSet recordSet, EntityMessages messages) {
        Assert.notNull(recordSet);

        if (!recordSet.entity().isChildEntity()) {
            return true;
        }

        if (recordSet.parent() == null) {
            throw new IllegalStateException("Entity \"" + recordSet.entity().getConceptAlias() + "\" is a child entity, but the RecordSet.parent() was null");
        }

        Set<String> parentIdentifiers = new HashSet<>();
        String parentIdentifierUri = recordSet.parent().entity().getUniqueKeyURI();

        for (Record r : recordSet.parent().records()) {
            parentIdentifiers.add(r.get(parentIdentifierUri));
        }

        String uri = recordSet.entity().getAttributeUri(recordSet.parent().entity().getUniqueKey());
        List<String> invalidIdentifiers = new LinkedList<>();

        for (Record r : recordSet.records()) {

            String value = r.get(uri);

            if (value.equals("") || !parentIdentifiers.contains(value)) {
                invalidIdentifiers.add(value);
            }

        }

        if (invalidIdentifiers.size() == 0) {
            return true;
        }

        setMessages(invalidIdentifiers, messages, recordSet.entity().getParentEntity());
        setError();
        return false;
    }

    private void setMessages(List<String> invalidValues, EntityMessages messages, String parentEntityAlias) {
        messages.addMessage(
                GROUP_MESSAGE,
                new Message(
                        "The following identifiers do not exist in the parent entity \"" + parentEntityAlias + "\": [\"" + String.join("\", \"", invalidValues) + "\"]"
                ),
                level()
        );
    }

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public boolean validConfiguration(List<String> messages, Entity entity) {
        return true;
    }
}
