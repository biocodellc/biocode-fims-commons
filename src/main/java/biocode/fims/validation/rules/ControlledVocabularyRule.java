package biocode.fims.validation.rules;

import biocode.fims.digester.Entity;
import biocode.fims.digester.Field;
import biocode.fims.models.records.Record;
import biocode.fims.models.records.RecordSet;
import biocode.fims.renderers.EntityMessages;
import biocode.fims.renderers.SimpleMessage;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Checks for values not in the {@link biocode.fims.digester.List} specified by the listName
 *
 * @author rjewing
 */
public class ControlledVocabularyRule extends SingleColumnRule {
    private static final String NAME = "ControlledVocabulary";
    @JsonProperty
    private String listName;
    private biocode.fims.digester.List list;

    // needed for RuleTypeIdResolver to dynamically instantiate Rule implementation
    ControlledVocabularyRule() {
    }

    public ControlledVocabularyRule(String column, String listName, RuleLevel level) {
        super(column, level);
        this.listName = listName;
    }

    public ControlledVocabularyRule(String column, String listName) {
        this(column, listName, RuleLevel.WARNING);
    }

    @Override
    public boolean run(RecordSet recordSet, EntityMessages messages) {
        Assert.notNull(recordSet);

        if (!validConfiguration(recordSet, messages)) {
            return false;
        }

        String uri = recordSet.entity().getAttributeUri(column);
        List<String> fields = getListFields();

        List<String> invalidValues = new ArrayList<>();

        for (Record r : recordSet.records()) {

            String value = r.get(uri);

            if (list.getCaseInsensitive()) {

                if (!fields.contains(value.toLowerCase())) {
                    invalidValues.add(value);
                }
            } else {

                if (!fields.contains(value)) {
                    invalidValues.add(value);
                }
            }

        }

        if (invalidValues.size() == 0) {
            return true;
        }

        setMessages(invalidValues, messages);
        setError();
        return false;
    }

    private List<String> getListFields() {
        List<String> listFields = new ArrayList<>();

        for (Field f: list.getFields()) {
            if (list.getCaseInsensitive()) {
                listFields.add(f.getValue().toLowerCase());
            } else {
                listFields.add(f.getValue());
            }
        }

        return listFields;
    }

    private void setMessages(List<String> invalidValues, EntityMessages messages) {
        for (String value: invalidValues) {
            messages.addMessage(
                    "\"" + column + "\" contains value not in list \"" + listName + "\"",
                    new SimpleMessage("\"" + value + "\" not an approved \"" + column + "\""),
                    level()
            );
        }
    }

    @Override
    public boolean validConfiguration(List<String> messages, Entity entity) {
        if (config == null) {
            throw new IllegalStateException("config must not be null. call setConfig first");
        }

        boolean valid = super.validConfiguration(messages, entity);

        if (StringUtils.isEmpty(listName)) {
            messages.add("Invalid " + name() + " Rule configuration. listName must not be blank or null.");

            return false;
        }

        list = config.getValidation().findList(listName);

        if (list == null) {
            messages.add("Invalid Project configuration. Could not find list with name \"" + listName + "\"");
            return false;
        }

        return valid;
    }

    @Override
    public String name() {
        return NAME;
    }
}
