package biocode.fims.projectConfig;

import biocode.fims.projectConfig.models.Attribute;
import biocode.fims.projectConfig.models.DataType;
import biocode.fims.projectConfig.models.Entity;
import biocode.fims.models.ExpeditionMetadataProperty;
import biocode.fims.validation.rules.*;
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
        mostAtomicWorksheetEntityHasUniqueKey();
        worksheetAttributesAreUnique();

        for (Entity e : config.entities()) {
            entityConceptAliasOnlyHasValidChars(e);
            entityHasConceptURI(e);
            entityWithWorksheetHasUniqueKey(e);
            entityUniqueKeysHaveMatchingAttribute(e);
            hashedEntityHasRequiredValue(e);
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

    private void hashedEntityHasRequiredValue(Entity e) {
        if (e.isHashed()) {
            for (Rule rule : e.getRules()) {
                if (rule.level() == RuleLevel.ERROR) {
                    if (rule instanceof RequiredValueRule) {
                        LinkedHashSet<String> columns = ((RequiredValueRule) rule).columns();
                        if (columns.size() > 1 || !columns.contains(e.getUniqueKey())) {
                            return;
                        }
                    } else if (rule instanceof RequiredValueInGroupRule &&
                            !((RequiredValueInGroupRule) rule).columns().contains(e.getUniqueKey())) {
                        return;
                    }
                }
            }

            errorMessages.add("Entity \"" + e.getConceptAlias() + "\" is a hashed entity, but is missing at least 1 RequiredValueRule with level = \"ERROR\" and a column that is not the uniqueKey \"" + e.getUniqueKey() + "\"");
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
            } else if (!e.getAttributeUri(parentEntity.getUniqueKey()).equals(parentEntity.getUniqueKeyURI())) {
                errorMessages.add("Entity \"" + e.getConceptAlias() + "\" specifies a parent entity but the attribute for " +
                        "the parent entity uniqueKey: \"" + parentEntity.getUniqueKey() + "\" has a different uri: \"" +
                        e.getAttributeUri(parentEntity.getUniqueKey()) + "\" instead of \"" + parentEntity.getUniqueKeyURI() + "\"");
            } else if (parentEntity.equals(e)) {
                errorMessages.add("Entity \"" + e.getConceptAlias() + "\" specifies a parent entity that is itself");
            } else if (e.getUniqueAcrossProject() && !parentEntity.getUniqueAcrossProject()) {
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

            if (rule instanceof UniqueValueRule) {
                UniqueValueRule r = (UniqueValueRule) rule;
                if (r.uniqueAcrossProject() && Objects.equals(r.column(), e.getUniqueKey()) && !e.getUniqueAcrossProject()) {
                    messages.add("UniqueValueRule for uniqueKey column: \"" + e.getUniqueKey() + "\" has uniqueAcrossProject = true, however entity: \"" + e.getConceptAlias() + "\" uniqueAcrossProject = false");
                }
            }
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

    private void mostAtomicWorksheetEntityHasUniqueKey() {
        Map<String, List<Entity>> sheetEntities = new HashMap<>();

        for (Entity e : config.entities()) {
            if (e.hasWorksheet()) {
                String worksheet = e.getWorksheet();
                sheetEntities.computeIfAbsent(worksheet, k -> new ArrayList<>()).add(e);
            }
        }

        for (Map.Entry<String, List<Entity>> entry : sheetEntities.entrySet()) {
            Entity mostAtomicEntity = null;

            for (Entity e : entry.getValue()) {
                if (mostAtomicEntity == null) {
                    mostAtomicEntity = e;
                    continue;
                }

                if (config.isEntityChildDescendent(mostAtomicEntity, e)) {
                    mostAtomicEntity = e;
                }
            }

            if (mostAtomicEntity.isHashed()) {
                errorMessages.add(
                        "Entity \"" + mostAtomicEntity.getConceptAlias() + "\" is the most atomic (child) entity in the worksheet: \"" + entry.getKey() + "\". This entity can not be a hashed entity."
                );
            }
        }

    }

    private void worksheetAttributesAreUnique() {
        Map<String, List<Entity>> sheetEntities = new HashMap<>();

        for (Entity e : config.entities()) {
            if (e.hasWorksheet()) {
                String worksheet = e.getWorksheet();
                sheetEntities.computeIfAbsent(worksheet, k -> new ArrayList<>()).add(e);
            }
        }

        for (Map.Entry<String, List<Entity>> entry : sheetEntities.entrySet()) {
            List<Entity> entities = entry.getValue();

            // short-circuit b/c duplicate attributes for a single entity are checked elsewhere. no need to check 2x
            if (entities.size() == 1) continue;

            String worksheet = entry.getKey();
            Set<String> columns = new HashSet<>();
            Set<String> uris = new HashSet<>();

            // sort parent entities first
            entities.sort((a, b) -> {
                if (a.isChildEntity()) {
                    if (!b.isChildEntity() || Objects.equals(a.getParentEntity(), b.getConceptAlias())) return 1;
                } else if (b.isChildEntity()) {
                    return -1;
                }
                return 0;
            });

            for (Entity e : entities) {
                for (Attribute a : e.getAttributes()) {
                    boolean dupColumn = !columns.add(a.getColumn());
                    boolean dupUri = !uris.add(a.getUri());

                    // only duplicate columns/uris are acceptable for parent uniqueKey
                    if ((dupColumn || dupUri) && e.isChildEntity()) {
                        Entity parent = entities.stream()
                                .filter(entity -> Objects.equals(entity.getConceptAlias(), e.getParentEntity()))
                                .findFirst()
                                .orElse(null);

                        if (parent != null) {
                            dupColumn = dupColumn && !Objects.equals(parent.getUniqueKey(), a.getColumn());
                            dupUri = dupUri && !Objects.equals(parent.getUniqueKeyURI(), a.getUri());
                        }
                    }

                    if (dupColumn) {
                        errorMessages.add("Worksheet \"" + worksheet + "\" contains a duplicate column \"" + a.getColumn() + "\"");
                    }

                    if (dupUri) {
                        errorMessages.add("Worksheet \"" + worksheet + "\" contains a duplicate attribute uri \"" + a.getUri() + "\"");
                    }
                }
            }
        }

    }

    public List<String> errors() {
        return errorMessages;
    }
}
