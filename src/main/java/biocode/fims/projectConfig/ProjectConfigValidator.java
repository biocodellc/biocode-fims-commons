package biocode.fims.projectConfig;

import biocode.fims.digester.Attribute;
import biocode.fims.digester.DataType;
import biocode.fims.digester.Entity;
import biocode.fims.validation.rules.Rule;
import org.apache.commons.lang.StringUtils;
import org.springframework.util.Assert;

import java.util.*;

/**
 * class to validate @link(ProjectConfig) instances
 *
 * @author rjewing
 */
public class ProjectConfigValidator {
    private final ProjectConfig config;
    private List<String> errorMessages;

    public ProjectConfigValidator(ProjectConfig config) {
        Assert.notNull(config);
        this.config = config;
        this.errorMessages = new ArrayList<>();
    }

    public boolean isValid() {
        validateMapping();

        return errorMessages.isEmpty();
    }

    private void validateMapping() {
        allEntitiesHaveUniqueConceptAlias();
        allAttributesHaveUniqueUri();

        for (Entity e : config.getEntities()) {
            entityConceptAliasOnlyHasValidChars(e);
            entityWithWorksheetHasUniqueKey(e);
            entityUniqueKeysHaveMatchingAttribute(e);
            allChildEntitiesHaveValidParent(e);
            dateTimeAttributesHaveDataFormat(e);
            allRulesHaveValidConfiguration(e);
        }
    }

    private void entityConceptAliasOnlyHasValidChars(Entity e) {
        if (!StringUtils.isBlank(e.getConceptAlias())
                && !e.getConceptAlias().matches("^[a-zA-Z0-9_]+$")) {
            errorMessages.add("Entity conceptAlias contains one or more invalid characters. Only letters, digits, and _ are valid");
        }
    }

    private void allEntitiesHaveUniqueConceptAlias() {
        Set<String> uniqueConceptAlias = new HashSet<>();

        for (Entity e : config.getEntities()) {
            if (StringUtils.isEmpty(e.getConceptAlias())) {
                errorMessages.add("Entity is missing a conceptAlias");
            } else if (!uniqueConceptAlias.add(e.getConceptAlias())) {
                errorMessages.add("Duplicate entity conceptAlias detected \"" + e.getConceptAlias() + "\"");
            }
        }
    }

    private void entityWithWorksheetHasUniqueKey(Entity e) {
        if (!StringUtils.isEmpty(e.getWorksheet()) && StringUtils.isEmpty(e.getUniqueKey())) {
            errorMessages.add("Entity \"" + e.getConceptAlias() + "\" specifies a worksheet but is missing a uniqueKey");
        }
    }

    private void entityUniqueKeysHaveMatchingAttribute(Entity e) {
        if (!StringUtils.isBlank(e.getUniqueKey())
                && e.getAttributes().stream()
                .noneMatch(a -> e.getUniqueKey().equals(a.getColumn()))
                ) {
            errorMessages.add("Entity \"" + e.getConceptAlias() + "\" specifies a uniqueKey but can not find an Attribute with a matching column");

        }

    }

    private void allChildEntitiesHaveValidParent(Entity e) {
        if (e.isChildEntity()) {

            Entity parentEntity = config.getEntity(e.getParentEntity());

            if (parentEntity == null) {
                errorMessages.add("Entity \"" + e.getConceptAlias() + "\" specifies a parent entity that does not exist");
            } else if (StringUtils.isBlank(parentEntity.getUniqueKey())) {
                errorMessages.add("Entity \"" + e.getConceptAlias() + "\" specifies a parent entity that is missing a uniqueKey");
            } else if (e.getAttributeUri(parentEntity.getUniqueKey()) == null) {
                errorMessages.add("Entity \"" + e.getConceptAlias() + "\" specifies a parent entity but is missing an attribute for the parent entity uniqueKey");
            }
        }
    }

    private void dateTimeAttributesHaveDataFormat(Entity e) {
        List<DataType> dataTimeDataTypes = Arrays.asList(DataType.DATE, DataType.DATETIME, DataType.TIME);

        for (Attribute a : e.getAttributes()) {
            if (dataTimeDataTypes.contains(a.getDatatype())
                    && StringUtils.isEmpty(a.getDataformat())) {
                errorMessages.add("Entity \"" + e.getConceptAlias() + "\" specifies an attribute \""
                        + a.getUri() + "\" with dataType \"" + a.getDatatype() + "\" but is missing a dataFormat");
            }
        }
    }

    private void allRulesHaveValidConfiguration(Entity e) {
        List<String> messages = new ArrayList<>();

        for (Rule rule : e.getRules()) {
            rule.validConfiguration(messages, e);
        }

        errorMessages.addAll(messages);
    }

    private void allAttributesHaveUniqueUri() {

        for (Entity e : config.getEntities()) {
            Set<String> uris = new HashSet<>();
            for (Attribute a : e.getAttributes()) {
                if (!uris.add(a.getUri())) {
                    errorMessages.add(
                            "Duplicate Attribute uri \"" + a.getUri() + "\" found in entity \"" + e.getConceptAlias() + "\""
                    );
                }
            }
        }
    }

    public List<String> errors() {
        return errorMessages;
    }
}
