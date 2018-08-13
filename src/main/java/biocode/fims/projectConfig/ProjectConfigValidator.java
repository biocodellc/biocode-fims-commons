package biocode.fims.projectConfig;

import biocode.fims.projectConfig.models.Attribute;
import biocode.fims.projectConfig.models.DataType;
import biocode.fims.projectConfig.models.Entity;
import biocode.fims.models.ExpeditionMetadataProperty;
import biocode.fims.validation.rules.Rule;
import org.apache.commons.lang.StringUtils;
import org.springframework.util.Assert;

import java.util.*;
import java.util.regex.Pattern;

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
        // allow entities to dynamic configure themselves
        config.entities().forEach(e -> e.configure(config));

        allEntitiesHaveUniqueConceptAlias();
        allAttributesHaveUniqueAndValidUri();
        allAttributesHaveUniqueColumn();
        allExpeditionMetadataHaveName();

        for (Entity e : config.entities()) {
            entityConceptAliasOnlyHasValidChars(e);
            entityHasConceptURI(e);
            entityWithWorksheetHasUniqueKey(e);
            entityUniqueKeysHaveMatchingAttribute(e);
            allChildEntitiesHaveValidParent(e);
            dateTimeAttributesHaveDataFormat(e);
            allRulesHaveValidConfiguration(e);
            checkIsValid(e);
        }
    }

    private void entityConceptAliasOnlyHasValidChars(Entity e) {
        if (!StringUtils.isBlank(e.getConceptAlias())
                && !e.getConceptAlias().matches("^[a-zA-Z0-9_]+$")) {
            errorMessages.add("Entity conceptAlias contains one or more invalid characters. Only letters, digits, and _ are valid");
        }
    }

    private void entityHasConceptURI(Entity e) {
        if (StringUtils.isBlank(e.getConceptURI())) {
            errorMessages.add("Entity \"" + e.getConceptAlias() + "\" is missing a conceptURI");
        }
    }

    private void allEntitiesHaveUniqueConceptAlias() {
        Set<String> uniqueConceptAlias = new HashSet<>();

        for (Entity e : config.entities()) {
            if (StringUtils.isEmpty(e.getConceptAlias())) {
                errorMessages.add("Entity is missing a conceptAlias");
            } else if (!uniqueConceptAlias.add(e.getConceptAlias().toLowerCase())) {
                errorMessages.add("Duplicate entity conceptAlias detected \"" + e.getConceptAlias() + "\". conceptAliases are not case sensitive.");
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

            Entity parentEntity = config.entity(e.getParentEntity());

            if (parentEntity == null) {
                errorMessages.add("Entity \"" + e.getConceptAlias() + "\" specifies a parent entity that does not exist");
            } else if (StringUtils.isBlank(parentEntity.getUniqueKey())) {
                errorMessages.add("Entity \"" + e.getConceptAlias() + "\" specifies a parent entity that is missing a uniqueKey");
            } else if (e.getAttributeUri(parentEntity.getUniqueKey()) == null) {
                errorMessages.add("Entity \"" + e.getConceptAlias() + "\" specifies a parent entity but is missing an attribute for " +
                        "the parent entity uniqueKey: \"" + parentEntity.getUniqueKey() + "\"");
            } else if (parentEntity.equals(e)) {
                errorMessages.add("Entity \"" + e.getConceptAlias() + "\" specifies a parent entity that is itself");
            } else if (e.getUniqueAcrossProject() && ! parentEntity.getUniqueAcrossProject()) {
                errorMessages.add("Entity \"" + e.getConceptAlias() + "\" requires the key to be unique across the entire project, but the parentEntity is not unique across the project.");
            }
        }
    }

    private void dateTimeAttributesHaveDataFormat(Entity e) {
        List<DataType> dataTimeDataTypes = Arrays.asList(DataType.DATE, DataType.DATETIME, DataType.TIME);

        for (Attribute a : e.getAttributes()) {
            if (dataTimeDataTypes.contains(a.getDataType())
                    && StringUtils.isEmpty(a.getDataFormat())) {
                errorMessages.add("Entity \"" + e.getConceptAlias() + "\" specifies an attribute \""
                        + a.getUri() + "\" with dataType \"" + a.getDataType() + "\" but is missing a dataFormat");
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

    private void checkIsValid(Entity e) {
        if (!e.isValid(config)) {
            errorMessages.addAll(e.validationErrorMessages());
        }
    }

    private void allAttributesHaveUniqueAndValidUri() {
        Pattern p = Pattern.compile("^[a-zA-Z0-9_:/]+$");
        for (Entity e : config.entities()) {
            Set<String> uris = new HashSet<>();
            for (Attribute a : e.getAttributes()) {
                if (a.getUri() == null || !p.matcher(a.getUri()).matches()) {
                    errorMessages.add(
                            "Invalid Attribute uri \"" + a.getUri() + "\" found in entity \"" + e.getConceptAlias() + "\". " +
                                    "Uri must only contain alpha-numeric or _:/ characters."
                    );
                }
                if (!uris.add(a.getUri())) {
                    errorMessages.add(
                            "Duplicate Attribute uri \"" + a.getUri() + "\" found in entity \"" + e.getConceptAlias() + "\""
                    );
                }
            }
        }
    }

    private void allAttributesHaveUniqueColumn() {
        for (Entity e : config.entities()) {
            Set<String> columns = new HashSet<>();
            for (Attribute a : e.getAttributes()) {
                if (!columns.add(a.getColumn())) {
                    errorMessages.add(
                            "Duplicate Attribute column \"" + a.getColumn() + "\" found in entity \"" + e.getConceptAlias() + "\""
                    );
                }
            }
        }
    }

    private void allExpeditionMetadataHaveName() {
        for (ExpeditionMetadataProperty p : config.expeditionMetadataProperties()) {
            if (StringUtils.isBlank(p.getName())) {
                errorMessages.add("ExpeditionMetadataProperty is missing a name.");
            }
        }
    }

    public List<String> errors() {
        return errorMessages;
    }
}
