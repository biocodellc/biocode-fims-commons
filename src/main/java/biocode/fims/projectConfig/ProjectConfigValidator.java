package biocode.fims.projectConfig;

import biocode.fims.digester.Attribute;
import biocode.fims.digester.DataType;
import biocode.fims.digester.Entity;
import biocode.fims.validation.rules.Rule;
import org.apache.commons.lang.StringUtils;

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
        this.config = config;
        this.errorMessages = new ArrayList<>();
    }

    public boolean isValid() {
        if (config.getMapping() == null) {
            return false;
        }

        validateMapping();

        return errorMessages.isEmpty();
    }

    private void validateMapping() {
        allEntitiesHaveUniqueConceptAlias();
        entityWithWorksheetHaveUniqueKey();
        entityUniqueKeysHaveMatchingAttribute();
        allChildEntitiesHaveValidParent();
        dateTimeAttributesHaveDataFormat();
        allRulesHaveValidConfiguration();
        allAttributesHaveUniqueUri();
    }

    private void allEntitiesHaveUniqueConceptAlias() {
        Set<String> uniqueConceptAlias = new HashSet<>();

        for (Entity e : config.getMapping().getEntities()) {
            if (StringUtils.isEmpty(e.getConceptAlias())) {
                errorMessages.add("Entity is missing a conceptAlias");
            } else if (!uniqueConceptAlias.add(e.getConceptAlias())) {
                errorMessages.add("Duplicate entity conceptAlias detected \"" + e.getConceptAlias() + "\"");
            }
        }
    }

    private void entityWithWorksheetHaveUniqueKey() {
        for (Entity e : config.getMapping().getEntities()) {
            if (!StringUtils.isEmpty(e.getWorksheet()) && StringUtils.isEmpty(e.getUniqueKey())) {
                errorMessages.add("Entity \"" + e.getConceptAlias() + "\" specifies a worksheet but is missing a uniqueKey");
            }
        }
    }

    private void entityUniqueKeysHaveMatchingAttribute() {
        for (Entity e : config.getMapping().getEntities()) {

            if (!StringUtils.isBlank(e.getUniqueKey())
                    && e.getAttributes().stream()
                    .noneMatch(a -> e.getUniqueKey().equals(a.getColumn()))
                    ) {
                errorMessages.add("Entity \"" + e.getConceptAlias() + "\" specifies a uniqueKey but can not find an Attribute with a matching column");

            }

        }

    }

    private void allChildEntitiesHaveValidParent() {
        for (Entity e : config.getMapping().getEntities()) {
            if (e.isChildEntity()) {

                Entity parentEntity = config.getMapping().getEntity(e.getParentEntity());

                if (parentEntity == null) {
                    errorMessages.add("Entity \"" + e.getConceptAlias() + "\" specifies a parent entity that does not exist");
                } else if (StringUtils.isBlank(parentEntity.getUniqueKey())) {
                    errorMessages.add("Entity \"" + e.getConceptAlias() + "\" specifies a parent entity that is missing a uniqueKey");
                } else if (e.getAttributes().stream().noneMatch(a -> a.getUri().equals(parentEntity.getUniqueKeyURI()))) {
                    errorMessages.add("Entity \"" + e.getConceptAlias() + "\" specifies a parent entity but is missing an attribute for the parent entity uniqueKey");
                }
            }
        }
    }

    private void dateTimeAttributesHaveDataFormat() {
        List<DataType> dataTimeDataTypes = Arrays.asList(DataType.DATE, DataType.DATETIME, DataType.TIME);

        for (Entity e : config.getMapping().getEntities()) {
            for (Attribute a : e.getAttributes()) {
                if (dataTimeDataTypes.contains(a.getDatatype())
                        && StringUtils.isEmpty(a.getDataformat())) {
                    errorMessages.add("Entity \"" + e.getConceptAlias() + "\" specifies an attribute \""
                            + a.getUri() + "\" with dataType \"" + a.getDatatype() + "\" but is missing a dataFormat");
                }
            }
        }
    }

    private void allRulesHaveValidConfiguration() {
        List<String> messages = new ArrayList<>();

        for (Entity e: config.getMapping().getEntities()) {
            for (Rule rule: e.getRules()) {
                rule.setConfig(config);
                rule.validConfiguration(messages, e);
            }
        }

        errorMessages.addAll(messages);
    }

    private void allAttributesHaveUniqueUri() {
        Set<String> uris = new HashSet<>();
        Map<String, List<String>> duplicateUris = new HashMap<>();
        Map<String, String> uriEntityMap = new HashMap<>();

        for (Entity e: config.getMapping().getEntities()) {
            for (Attribute a: e.getAttributes()) {
                if (!uris.add(a.getUri())) {
                    duplicateUris.computeIfAbsent(a.getUri(), k -> new ArrayList<>()).add(e.getConceptAlias());
                } else {
                    uriEntityMap.put(a.getUri(), e.getConceptAlias());
                }
            }
        }

        if (duplicateUris.size() > 0) {
            for (Map.Entry<String, List<String>> entry: duplicateUris.entrySet()) {
                String uri = entry.getKey();
                List<String> entities = entry.getValue();
                entities.add(uriEntityMap.get(uri)); // add the 1st entity to the list so we can report all entities to user

                errorMessages.add(
                        "Attribute uris must be unique. Duplicate uri \"" + uri + "\" found in entities: [\"" +
                                String.join("\", \"", entities) + "\"]"
                );
            }
        }
    }

    public List<String> errors() {
        return errorMessages;
    }
}
