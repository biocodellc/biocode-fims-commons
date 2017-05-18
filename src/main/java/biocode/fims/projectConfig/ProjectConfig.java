package biocode.fims.projectConfig;

import biocode.fims.digester.*;
import biocode.fims.fimsExceptions.FimsRuntimeException;
import biocode.fims.fimsExceptions.errorCodes.ConfigCode;
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
        for (biocode.fims.digester.List list : lists) {
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
        for (Entity entity : entities) {
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

    public boolean areRelatedEntities(String conceptAlias1, String conceptAlias2) {

        Entity entity1 = getEntity(conceptAlias1);
        Entity entity2 = getEntity(conceptAlias2);

        if (entity1 == null || entity2 == null) {
            return false;
        }

        if (entity1.isChildEntity() && checkEntityRelation(entity1, entity2)) {
            return true;
        } else if (entity2.isChildEntity() && checkEntityRelation(entity2, entity1)) {
            return true;
        }

        return false;
    }

    private boolean checkEntityRelation(Entity childEntity, Entity elderEntity) {
        Entity parentEntity;
        do {
            parentEntity = getEntity(childEntity.getParentEntity());

            if (parentEntity.getConceptAlias().equals(elderEntity.getConceptAlias())) {
                return true;
            }

            childEntity = parentEntity;
        } while (parentEntity.isChildEntity());

        return false;
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

    public boolean isEntityChildDescendent(Entity elderEntity, Entity childEntity) {
        return childEntity.isChildEntity() && checkEntityRelation(childEntity, elderEntity);
    }

    /**
     * get of all entities that form the relation. Beginning with the elderEntity and ending with the childEntity
     *
     * If the entities are directly related the returned value would be [elderEntity, childEntity]
     * If the entities are not directly related the returned list would be [elderEntity, intermediaryEntity, ... ,childEntity]
     * If the entites are no related, an empty list will be returned
     *
     * @param elderEntity
     * @param childEntity
     * @return
     */
    public LinkedList<Entity> getEntitiesInRelation(Entity elderEntity, Entity childEntity) {
        LinkedList<Entity> relatedEntities = new LinkedList<>();
        boolean found = false;

        if (!entities.contains(elderEntity) || !entities.contains(childEntity)) {
            throw new FimsRuntimeException("Server Error", "Entity doesn't exist", 500);
        }

        relatedEntities.add(childEntity);

        Entity parentEntity;
        do {
            parentEntity = getEntity(childEntity.getParentEntity());
            relatedEntities.add(parentEntity);

            if (parentEntity.getConceptAlias().equals(elderEntity.getConceptAlias())) {
                found = true;
            }

            childEntity = parentEntity;
        } while (!found && parentEntity.isChildEntity());

        if (!found) {
            return new LinkedList<>();
        }

        Collections.reverse(relatedEntities);
        return relatedEntities;
    }
}
