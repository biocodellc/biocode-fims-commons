package biocode.fims.config.project;

import biocode.fims.config.Config;
import biocode.fims.config.network.NetworkConfig;
import biocode.fims.config.models.Attribute;
import biocode.fims.config.models.Entity;
import com.fasterxml.jackson.annotation.JsonIgnore;

import java.util.HashMap;
import java.util.Map;


/**
 * @author rjewing
 */
public class ProjectConfig extends Config {

    @JsonIgnore
    public boolean isValid(NetworkConfig networkConfig) {
        mergeNetworkConfig(networkConfig);
        ProjectConfigValidator validator = new ProjectConfigValidator(this, networkConfig);

        validated = true;
        if (!validator.isValid()) {
            this.errors = validator.errors();
            return false;
        }

        return true;
    }

    /**
     * Merge NetworkConfig into ProjectConfig to eliminate possible config errors
     *
     * @param networkConfig
     */
    private void mergeNetworkConfig(NetworkConfig networkConfig) {
        networkConfig.expeditionMetadataProperties().forEach(p -> {
            if (!expeditionMetadataProperties().contains(p)) expeditionMetadataProperties().add(p);
        });

        networkConfig.lists().forEach(l -> {
            if (!lists().contains(l)) addList(l);
        });

        networkConfig.entities().forEach(e -> {
            Entity entity = entity(e.getConceptAlias());
            if (entity == null) return;

            if (e.isHashed() && !entity.isHashed()) {
                entity.setHashed(true);
            }
            if (e.getUniqueAcrossProject() && !entity.getUniqueAcrossProject()) {
                entity.setUniqueAcrossProject(true);
            }
            if (entity.getUniqueKey() == null && !entity.isHashed()) {
                entity.setUniqueKey(e.getUniqueKey());
            }
            if (e.hasWorksheet() && entity.getWorksheet() == null) {
                entity.setWorksheet(e.getWorksheet());
            }
            entity.setConceptURI(e.getConceptURI());
            entity.setParentEntity(e.getParentEntity());
            entity.setRecordType(e.getRecordType());

            entity.addRules(e.getRules());

            Map<String, Attribute> networkAttributes = new HashMap<>();
            e.getAttributes().forEach(a -> networkAttributes.put(a.getUri(), a));

            entity.getAttributes().forEach(a -> {
                Attribute networkAttribute = networkAttributes.get(a.getUri());
                if (networkAttribute == null) return;

                a.setColumn(networkAttribute.getColumn());
                a.setDataType(networkAttribute.getDataType());
                a.setInternal(networkAttribute.isInternal());
                a.setDefinedBy(networkAttribute.getDefinedBy());
                a.setDataFormat(networkAttribute.getDataFormat());
                a.setDelimitedBy(networkAttribute.getDelimitedBy());

                if (a.getGroup() == null) {
                    a.setGroup(networkAttribute.getGroup());
                }
                if (a.getDefinition() == null) {
                    a.setDelimitedBy(networkAttribute.getDefinition());
                }
            });
        });

    }
}
