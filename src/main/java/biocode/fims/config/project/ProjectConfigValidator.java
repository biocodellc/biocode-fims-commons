package biocode.fims.config.project;

import biocode.fims.config.ConfigValidator;
import biocode.fims.config.network.NetworkConfig;
import biocode.fims.config.models.Attribute;
import biocode.fims.config.models.Entity;
import biocode.fims.models.ExpeditionMetadataProperty;
import biocode.fims.validation.rules.Rule;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * class to validate @link(ProjectConfig) instances
 *
 * @author rjewing
 */
public class ProjectConfigValidator extends ConfigValidator {

    private final NetworkConfig networkConfig;
    private Entity networkEntity;

    public ProjectConfigValidator(ProjectConfig config, NetworkConfig networkConfig) {
        super(config);
        this.networkConfig = networkConfig;
    }

    @Override
    protected void validateConfig() {
        hasValidValidationLists();
        containsNetworkExpeditionProps();
    }

    @Override
    protected void validateEntity(Entity e) {
        isValidNetworkEntity(e);
        if (networkEntity != null) {
            allAttributesAreValidNetworkAttributes(e);
            entityContainsNetworkRules(e);
            entityHasValidUniqueKey(e);
        }
        networkEntity = null;
    }

    private void hasValidValidationLists() {
        if (config.lists().equals(networkConfig.lists())) return;

        errorMessages.add("Project config validation lists differ from the network config validation lists");
    }

    private void containsNetworkExpeditionProps() {
        for (ExpeditionMetadataProperty p : networkConfig.expeditionMetadataProperties()) {
            if (!config.expeditionMetadataProperties().contains(p)) {
                errorMessages.add("Project config expeditionMetadataProperties is missing a network prop: \"" + p.getName() + "\"");
            }
        }
    }

    private void entityHasValidUniqueKey(Entity e) {
        if (e.isHashed()
                || networkEntity.getUniqueKey().equals(e.getUniqueKey())) {
            return;
        }

        if (e.isChildEntity()) {
            Entity parent = config.entity(e.getParentEntity());
            if (parent != null
                    && !parent.isHashed()
                    && !"".equals(parent.getUniqueKey())
                    && parent.getUniqueKey().equals(e.getUniqueKey())) {
                return;
            }
        }

        errorMessages.add("Entity \"" + e.getConceptAlias() + "\" does not specify a valid uniqueKey. The uniqueKey can be the network entity's uniqueKey or a parent entity's uniqueKey");
    }

    private void entityContainsNetworkRules(Entity e) {
        for (Rule r : networkEntity.getRules()) {
            boolean found = false;
            for (Rule rule : e.getRules()) {
                if (rule.equals(r) || rule.contains(r)) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                errorMessages.add("Entity \"" + e.getConceptAlias() + "\" is missing a network Rule: type: \"" + r.name() + "\", level: \"" + r.level() + "\"");
            }
        }
    }

    private void allAttributesAreValidNetworkAttributes(Entity e) {
        Map<String, Attribute> networkAttributes = new HashMap<>();
        networkEntity.getAttributes().forEach(a -> networkAttributes.put(a.getUri(), a));

        for (Attribute a : e.getAttributes()) {
            Attribute networkAttribute = networkAttributes.get(a.getUri());

            if (networkAttribute == null) {
                errorMessages.add("Entity \"" + e.getConceptAlias() + "\" contains an Attribute \"" + a.getUri() + "\" that is not found in the network entity");
                continue;
            }

            if (!networkAttribute.getColumn().equals(a.getColumn())) {
                errorMessages.add("Entity \"" + e.getConceptAlias() + "\" contains an Attribute \"" + a.getUri() + "\" whos column does not match the network Attribute's column");
            }
            if (!networkAttribute.getDataType().equals(a.getDataType())) {
                errorMessages.add("Entity \"" + e.getConceptAlias() + "\" contains an Attribute \"" + a.getUri() + "\" whos dataType does not match the network Attribute's dataType");
            }
            if (!Objects.equals(networkAttribute.getDataFormat(), a.getDataFormat())) {
                errorMessages.add("Entity \"" + e.getConceptAlias() + "\" contains an Attribute \"" + a.getUri() + "\" whos dataFormat does not match the network Attribute's dataFormat");
            }
            if (!networkAttribute.isInternal() == a.isInternal()) {
                errorMessages.add("Entity \"" + e.getConceptAlias() + "\" contains an Attribute \"" + a.getUri() + "\" whos internal property does not match the network Attribute's internal property");
            }
            if (!Objects.equals(networkAttribute.getDefinedBy(), a.getDefinedBy())) {
                errorMessages.add("Entity \"" + e.getConceptAlias() + "\" contains an Attribute \"" + a.getUri() + "\" whos definedBy does not match the network Attribute's definedBy");
            }
            if (!Objects.equals(networkAttribute.getDelimitedBy(), a.getDelimitedBy())) {
                errorMessages.add("Entity \"" + e.getConceptAlias() + "\" contains an Attribute \"" + a.getUri() + "\" whos delimitedBy does not match the network Attribute's delimitedBy");
            }
        }

    }

    private void isValidNetworkEntity(Entity e) {
        networkEntity = networkConfig.entity(e.getConceptAlias());

        if (networkEntity == null) {
            errorMessages.add("Entity \"" + e.getConceptAlias() + "\" is not a registered entity for this network");
            return;
        }

        if (!networkEntity.getConceptURI().equals(e.getConceptURI())) {
            errorMessages.add("Entity \"" + e.getConceptAlias() + "\".conceptUri does not match the network entity's conceptUri");
        }
        if (!Objects.equals(networkEntity.getParentEntity(), e.getParentEntity())) {
            errorMessages.add("Entity \"" + e.getConceptAlias() + "\".parentEntity does not match the network entity's parentEntity");
        }
        if (!networkEntity.getRecordType().equals(e.getRecordType())) {
            errorMessages.add("Entity \"" + e.getConceptAlias() + "\".recordType does not match the network entity's recordType");
        }
        if (!networkEntity.type().equals(e.type())) {
            errorMessages.add("Entity \"" + e.getConceptAlias() + "\".type does not match the network entity's type");
        }

    }
}

