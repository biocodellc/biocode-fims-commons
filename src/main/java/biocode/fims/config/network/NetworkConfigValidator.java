package biocode.fims.config.network;

import biocode.fims.config.ConfigValidator;
import biocode.fims.config.models.Attribute;
import biocode.fims.config.models.DataType;
import biocode.fims.config.models.Entity;
import org.apache.commons.lang3.StringUtils;

import java.util.*;
import java.util.regex.Pattern;

/**
 * class to validate @link(NetworkConfig) instances
 *
 * @author rjewing
 */
public class NetworkConfigValidator extends ConfigValidator {

    public NetworkConfigValidator(NetworkConfig config) {
        super(config);
    }

    @Override
    protected void validateConfig() {
        allEntitiesHaveUniqueConceptAlias();
        allAttributesHaveUniqueAndValidUri();
        allAttributesHaveUniqueColumn();
    }

    @Override
    protected void validateEntity(Entity e) {
        entityConceptAliasOnlyHasValidChars(e);
        entityHasConceptURI(e);
        allChildEntitiesHaveValidParent(e);
        dateTimeAttributesHaveDataFormat(e);
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

}
