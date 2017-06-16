package biocode.fims.projectConfig;

import biocode.fims.digester.*;
import biocode.fims.fimsExceptions.FimsRuntimeException;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.glassfish.jersey.server.JSONP;

import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author rjewing
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ProjectConfig {

    private final LinkedList<Entity> entities;
    @JsonProperty
    private final LinkedList<biocode.fims.digester.List> lists;
    private String expeditionForwardingAddress;
    private String datasetForwardingAddress;
    private List<String> errors;
    private boolean validated = false;

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
        Entity entity = entity(conceptAlias);

        return entity.hasWorksheet() && entitiesForSheet(entity.getWorksheet()).size() > 1;
    }

    public Entity entity(String conceptAlias) {
        for (Entity entity : entities) {
            if (entity.getConceptAlias().equals(conceptAlias)) {
                return entity;
            }
        }

        return null;
    }

    public List<Entity> entitiesForSheet(String sheetName) {
        return entities.stream()
                .filter(e -> sheetName.equals(e.getWorksheet()))
                .collect(Collectors.toList());
    }

    public boolean areRelatedEntities(String conceptAlias1, String conceptAlias2) {

        Entity entity1 = entity(conceptAlias1);
        Entity entity2 = entity(conceptAlias2);

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
            parentEntity = entity(childEntity.getParentEntity());

            if (parentEntity.getConceptAlias().equals(elderEntity.getConceptAlias())) {
                return true;
            }

            childEntity = parentEntity;
        } while (parentEntity.isChildEntity());

        return false;
    }


    @JsonProperty
    public LinkedList<Entity> entities() {
        return entities;
    }

    public void addEntity(Entity entity) {
        entities.add(entity);
        validated = false;
    }

    public void addList(biocode.fims.digester.List list) {
        lists.add(list);
        validated = false;
    }

    @JsonProperty
    public String expeditionForwardingAddress() {
        return expeditionForwardingAddress;
    }

    public void setExpeditionForwardingAddress(String expeditionForwardingAddress) {
        this.expeditionForwardingAddress = expeditionForwardingAddress;
    }

    @JsonProperty
    public String datasetForwardingAddress() {
        return datasetForwardingAddress;
    }

    public void setDatasetForwardingAddress(String datasetForwardingAddress) {
        this.datasetForwardingAddress = datasetForwardingAddress;
    }

    @JsonIgnore
    public List<String> errors() {
        if (!validated) {
            isValid();
        }
        return (errors != null) ? errors : new ArrayList<>();
    }

    public boolean isEntityChildDescendent(Entity elderEntity, Entity childEntity) {
        return childEntity.isChildEntity() && checkEntityRelation(childEntity, elderEntity);
    }

    /**
     * get of all entities that form the relation. Beginning with the elderEntity and ending with the childEntity
     *
     * If the entities are directly related the returned value would be [elderEntity, childEntity]
     * If the entities are not directly related the returned list would be [elderEntity, intermediaryEntity, ... ,childEntity]
     * If the entites are not related, an empty list will be returned
     *
     * @param elderEntity
     * @param childEntity
     * @return
     */
    public LinkedList<Entity> entitiesInRelation(Entity elderEntity, Entity childEntity) {
        LinkedList<Entity> relatedEntities = new LinkedList<>();
        boolean found = false;

        if (!entities.contains(elderEntity) || !entities.contains(childEntity)) {
            throw new FimsRuntimeException("Server Error", "Entity doesn't exist", 500);
        }

        relatedEntities.add(childEntity);

        Entity parentEntity;
        do {
            parentEntity = entity(childEntity.getParentEntity());

            if (parentEntity == null) {
                break;
            }

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

    public void generateUris() {
        for (Entity e: entities) {
            e.generateUris();
        }
    }

    @JsonIgnore
    public boolean isValid() {
        ProjectConfigValidator validator = new ProjectConfigValidator(this);

        validated = true;
        if (!validator.isValid()) {
            this.errors = validator.errors();
            return false;
        }

        return true;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ProjectConfig)) return false;

        ProjectConfig config = (ProjectConfig) o;

        if (!entities.equals(config.entities)) return false;
        if (!lists.equals(config.lists)) return false;
        if (expeditionForwardingAddress != null ? !expeditionForwardingAddress.equals(config.expeditionForwardingAddress) : config.expeditionForwardingAddress != null)
            return false;
        return datasetForwardingAddress != null ? datasetForwardingAddress.equals(config.datasetForwardingAddress) : config.datasetForwardingAddress == null;
    }

    @Override
    public int hashCode() {
        int result = entities.hashCode();
        result = 31 * result + lists.hashCode();
        result = 31 * result + (expeditionForwardingAddress != null ? expeditionForwardingAddress.hashCode() : 0);
        result = 31 * result + (datasetForwardingAddress != null ? datasetForwardingAddress.hashCode() : 0);
        return result;
    }
}
