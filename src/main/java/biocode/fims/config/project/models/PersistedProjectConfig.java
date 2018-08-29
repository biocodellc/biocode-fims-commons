package biocode.fims.config.project.models;

import biocode.fims.config.models.Entity;
import biocode.fims.config.project.ProjectConfig;
import biocode.fims.models.ExpeditionMetadataProperty;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.stream.Collectors;

/**
 * @author rjewing
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PersistedProjectConfig {

    @JsonProperty
    private final LinkedList<ProjectEntity> entities;
    @JsonProperty
    private final java.util.List<ExpeditionMetadataProperty> expeditionMetadataProperties;

    private PersistedProjectConfig() {
        this.entities = new LinkedList<>();
        this.expeditionMetadataProperties = new ArrayList<>();
    }

    public static PersistedProjectConfig newInstance() {
        return new PersistedProjectConfig();
    }

    public static PersistedProjectConfig fromProjectConfig(ProjectConfig projectConfig) {
        projectConfig.addDefaultRules();
        PersistedProjectConfig config = new PersistedProjectConfig();

        config.expeditionMetadataProperties.addAll(
                projectConfig.expeditionMetadataProperties().stream()
                        .filter(p -> !p.isNetworkProp())
                        .collect(Collectors.toList())
        );

        for (Entity e : projectConfig.entities()) {
            config.entities.add(new ProjectEntity(e));
        }

        return config;
    }

    public ProjectConfig toProjectConfig(biocode.fims.config.network.NetworkConfig networkConfig) {
        ProjectConfig config = new ProjectConfig();

        config.setExpeditionMetadataProperties(networkConfig.expeditionMetadataProperties());
        config.expeditionMetadataProperties().forEach(p -> p.setNetworkProp(true));
        config.expeditionMetadataProperties().addAll(expeditionMetadataProperties);

        config.lists().addAll(networkConfig.lists());

        for (ProjectEntity pe : entities) {
            Entity networkEntity = networkConfig.entity(pe.getConceptAlias());
            config.addEntity(pe.toEntity(networkEntity));
        }

        config.addDefaultRules();
        return config;
    }


}
