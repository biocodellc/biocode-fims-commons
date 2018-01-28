package biocode.fims.validation.rules;

import biocode.fims.digester.Entity;
import biocode.fims.digester.Field;
import biocode.fims.models.records.Record;
import biocode.fims.models.records.RecordSet;
import biocode.fims.projectConfig.ProjectConfig;
import biocode.fims.validation.messages.EntityMessages;
import biocode.fims.validation.messages.ListMessage;
import biocode.fims.validation.messages.Message;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonStreamContext;
import com.fasterxml.jackson.databind.DeserializationConfig;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.PropertyMetadata;
import com.fasterxml.jackson.databind.PropertyName;
import com.fasterxml.jackson.databind.annotation.JsonValueInstantiator;
import com.fasterxml.jackson.databind.deser.CreatorProperty;
import com.fasterxml.jackson.databind.deser.SettableBeanProperty;
import com.fasterxml.jackson.databind.deser.ValueInstantiator.Base;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Checks for values not in the {@link biocode.fims.digester.List} specified by the listName
 *
 * @author rjewing
 */
@JsonValueInstantiator(ControlledVocabularyRule.ValueInstantiator.class)
public class ControlledVocabularyRule extends SingleColumnRule {
    private static final String NAME = "ControlledVocabulary";
    @JsonProperty
    private String listName;
    @JsonIgnore
    private ProjectConfig config;

    private biocode.fims.digester.List list;

    // needed for RuleTypeIdResolver to dynamically instantiate Rule implementation
    private ControlledVocabularyRule() {
    }

    public ControlledVocabularyRule(String column, String listName, ProjectConfig config, RuleLevel level) {
        super(column, level);
        this.listName = listName;
        this.config = config;
    }

    public ControlledVocabularyRule(String column, String listName, ProjectConfig config) {
        this(column, listName, config, RuleLevel.WARNING);
    }

    @Override
    public boolean run(RecordSet recordSet, EntityMessages messages) {
        Assert.notNull(recordSet);

        if (!validConfiguration(recordSet, messages)) {
            return false;
        }

        String uri = recordSet.entity().getAttributeUri(column);
        List<String> fields = getListFields();

        Set<String> invalidValues = new LinkedHashSet<>();

        for (Record r : recordSet.records()) {

            String value = r.get(uri);

            if (value.equals("")) {
                continue;
            }

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

        for (Field f : list.getFields()) {
            if (list.getCaseInsensitive()) {
                listFields.add(f.getValue().toLowerCase());
            } else {
                listFields.add(f.getValue());
            }
        }

        return listFields;
    }

    private void setMessages(Set<String> invalidValues, EntityMessages messages) {
        List<String> fields = list.getFields()
                .stream()
                .map(Field::getValue)
                .collect(Collectors.toList());;

        for (String value : invalidValues) {
            messages.addMessage(
                    "Unapproved value(s)",
                    new ListMessage(fields, "\"" + value + "\" in column \"" + column + "\" not in list \"" + listName + "\""),
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

        list = config.findList(listName);

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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ControlledVocabularyRule)) return false;
        if (!super.equals(o)) return false;

        ControlledVocabularyRule that = (ControlledVocabularyRule) o;

        return listName != null ? listName.equals(that.listName) : that.listName == null;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (listName != null ? listName.hashCode() : 0);
        return result;
    }


    /**
     * class to inject ProjectConfig into ControlledVocabularyRule from parent context during deserialization
     */
    public static class ValueInstantiator extends Base {

        public ValueInstantiator() {
            super(ControlledVocabularyRule.class);
        }

        @Override
        public SettableBeanProperty[] getFromObjectArguments(DeserializationConfig config) {
            CreatorProperty column = new CreatorProperty(
                    new PropertyName("column"),
                    config.constructType(String.class),
                    null, null, null, null, 0, null, PropertyMetadata.STD_REQUIRED);
            CreatorProperty listName = new CreatorProperty(
                    new PropertyName("listName"),
                    config.constructType(String.class),
                    null, null, null, null, 1, null, PropertyMetadata.STD_REQUIRED);
            CreatorProperty projectConfig = new CreatorProperty(
                    new PropertyName("config"),
                    config.constructType(ProjectConfig.class),
                    null, null, null, null, 2, null, PropertyMetadata.STD_REQUIRED_OR_OPTIONAL);
            CreatorProperty level = new CreatorProperty(
                    new PropertyName("level"),
                    config.constructType(RuleLevel.class),
                    null, null, null, null, 3, null, PropertyMetadata.STD_REQUIRED_OR_OPTIONAL);

            return new SettableBeanProperty[]{column, listName, level, projectConfig};
        }

        @Override
        public boolean canCreateFromObjectWith() {
            return true;
        }


        @Override
        public Object createFromObjectWith(DeserializationContext ctxt, Object[] args) throws IOException {
            ProjectConfig config = (ProjectConfig) args[2];
            if (config == null) {
                config = getParentProjectConfig(ctxt.getParser().getParsingContext());
            }

            return new ControlledVocabularyRule((String) args[0], (String) args[1], config, (RuleLevel) args[3]);
        }

        private ProjectConfig getParentProjectConfig(JsonStreamContext ctxt) {
            if (ctxt == null) {
                return null;
            }

            if (ctxt.getCurrentValue() instanceof ProjectConfig) {
                return (ProjectConfig) ctxt.getCurrentValue();
            }

            return getParentProjectConfig(ctxt.getParent());
        }
    }
}