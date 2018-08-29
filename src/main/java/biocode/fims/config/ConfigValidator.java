package biocode.fims.config;

import biocode.fims.config.models.Attribute;
import biocode.fims.config.models.Entity;
import biocode.fims.models.ExpeditionMetadataProperty;
import biocode.fims.validation.rules.*;
import org.apache.commons.lang.StringUtils;
import org.springframework.util.Assert;

import java.util.*;

/**
 * Base Config validator, designed to be extended to provide additional validation rules
 *
 * @author rjewing
 */
public abstract class ConfigValidator {
    protected final Config config;
    protected List<String> errorMessages;

    public ConfigValidator(Config config) {
        Assert.notNull(config, "config must not be null");
        this.config = config;
        this.errorMessages = new ArrayList<>();
    }

    public boolean isValid() {
        validate();

        return errorMessages.isEmpty();
    }

    public void validate() {
        // allow entities to dynamic configure themselves
        config.entities().forEach(e -> e.configure(config));

        validateConfig();
        mostAtomicWorksheetEntityHasUniqueKey();
        worksheetAttributesAreUnique();
        allExpeditionMetadataHaveName();

        for (Entity e : config.entities()) {
            entityWithWorksheetHasUniqueKey(e);
            entityUniqueKeysHaveMatchingAttribute(e);
            hashedEntityHasRequiredValue(e);
            allRulesHaveValidConfiguration(e);
            validateEntity(e);
            checkIsValid(e);
        }
    }

    protected abstract void validateConfig();

    protected abstract void validateEntity(Entity entity);

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

    private void checkIsValid(Entity e) {
        if (!e.isValid(config)) {
            errorMessages.addAll(e.validationErrorMessages());
        }
    }
}
