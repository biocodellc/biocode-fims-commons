package biocode.fims.projectConfig;

import biocode.fims.digester.*;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author rjewing
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ProjectConfig {

    private final LinkedList<Entity> entities;
    @JsonProperty
    private final LinkedList<biocode.fims.digester.List> lists;
    private String expeditionForwardingAddress;
    private String datasetForwardingAddress;
    private String description;

    public ProjectConfig() {
        this.entities = new LinkedList<>();
        this.lists = new LinkedList<>();
    }

    /**
     * Lookup a {@link biocode.fims.digester.List} by its alias
     *
     * @param alias
     * @return
     */
    public biocode.fims.digester.List findList(String alias) {
        for (biocode.fims.digester.List list: lists) {
            if (list.getAlias().equals(alias)) {
                return list;
            }
        }

        return null;
    }

    public boolean isMultiSheetEntity(String conceptAlias) {
        Entity entity = getEntity(conceptAlias);

        return entity.hasWorksheet() && getEntitiesForSheet(entity.getWorksheet()).size() > 1;
    }

    public Entity getEntity(String conceptAlias) {
        for (Entity entity: entities) {
            if (entity.getConceptAlias().equals(conceptAlias)) {
                return entity;
            }
        }

        return null;
    }

    public List<Entity> getEntitiesForSheet(String sheetName) {
        return entities.stream()
                .filter(e -> sheetName.equals(e.getWorksheet()))
                .collect(Collectors.toList());
    }

    public LinkedList<Entity> getEntities() {
        return entities;
    }

    public void addEntity(Entity entity) {
        entities.add(entity);
    }

    public void addList(biocode.fims.digester.List list) {
        lists.add(list);
    }

    public String getExpeditionForwardingAddress() {
        return expeditionForwardingAddress;
    }

    public void setExpeditionForwardingAddress(String expeditionForwardingAddress) {
        this.expeditionForwardingAddress = expeditionForwardingAddress;
    }

    public String getDatasetForwardingAddress() {
        return datasetForwardingAddress;
    }

    public void setDatasetForwardingAddress(String datasetForwardingAddress) {
        this.datasetForwardingAddress = datasetForwardingAddress;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
