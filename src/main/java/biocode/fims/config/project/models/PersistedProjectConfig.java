package biocode.fims.config.project.models;

import biocode.fims.config.models.Entity;
import biocode.fims.config.models.List;
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
    private final LinkedList<List> lists;
    @JsonProperty
    private final java.util.List<ExpeditionMetadataProperty> expeditionMetadataProperties;

    private PersistedProjectConfig() {
        this.entities = new LinkedList<>();
        this.lists = new LinkedList<>();
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

        projectConfig.entities().forEach(e -> config.entities.add(new ProjectEntity(e)));

        projectConfig.lists().stream()
                .filter(l -> !l.isNetworkList())
                .forEach(config.lists::add);

        return config;
    }

    public ProjectConfig toProjectConfig(biocode.fims.config.network.NetworkConfig networkConfig) {
        ProjectConfig config = new ProjectConfig();

        config.setExpeditionMetadataProperties(networkConfig.expeditionMetadataProperties());
        config.expeditionMetadataProperties().forEach(p -> p.setNetworkProp(true));
        config.expeditionMetadataProperties().addAll(expeditionMetadataProperties);

        for (ProjectEntity pe : entities) {
            Entity networkEntity = networkConfig.entity(pe.getConceptAlias());
            config.addEntity(pe.toEntity(networkEntity));
        }

        config.lists().addAll(networkConfig.lists());
        config.lists().addAll(lists);

        config.addDefaultRules();
        return config;
    }


}
