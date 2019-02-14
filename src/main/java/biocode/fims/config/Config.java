package biocode.fims.config;

import biocode.fims.config.models.Attribute;
import biocode.fims.config.models.Entity;
import biocode.fims.fimsExceptions.FimsRuntimeException;
import biocode.fims.fimsExceptions.errorCodes.ConfigCode;
import biocode.fims.models.EntityRelation;
import biocode.fims.models.ExpeditionMetadataProperty;
import biocode.fims.validation.rules.RequiredValueRule;
import biocode.fims.validation.rules.RuleLevel;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Subclasses must set the validated property when the config has been validated.
 *
 * @author rjewing
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public abstract class Config {

    protected final LinkedList<Entity> entities;
    protected final LinkedList<biocode.fims.config.models.List> lists;
    protected List<ExpeditionMetadataProperty> expeditionMetadataProperties;
    protected List<String> errors;
    protected boolean validated = false;

    public Config() {
        this.entities = new LinkedList<>();
        this.lists = new LinkedList<>();
        this.expeditionMetadataProperties = new ArrayList<>();
    }

    /**
     * Lookup a {@link biocode.fims.config.models.List} by its alias
     *
     * @param alias
     * @return
     */
    public biocode.fims.config.models.List findList(String alias) {
        for (biocode.fims.config.models.List list : lists) {
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

    public List<Attribute> attributesForSheet(String sheetName) {
        return entities.stream()
                .filter(e -> sheetName.equals(e.getWorksheet()))
                .flatMap(e -> e.getAttributes().stream())
                .collect(Collectors.toList());
    }

    public boolean areRelatedEntities(String conceptAlias1, String conceptAlias2) {

        Entity entity1 = entity(conceptAlias1);
        Entity entity2 = entity(conceptAlias2);

        if (entity1 == null || entity2 == null) {
            return false;
        }

        if (entity1.equals(entity2)) return false;

        // if entities have a common ancestor, then they can be considered related
        ArrayList<Entity> entity1Parents = parentEntities(entity1.getConceptAlias());

        if (entity1Parents.contains(entity2)) return true;

        for (Entity entity : parentEntities(entity2.getConceptAlias())) {

            if (entity1Parents.contains(entity) || entity1.equals(entity)) {
                return true;
            }
        }

        return false;
    }

    /**
     * check up the entity chain to determine if childEntity is a descendant of elderEntity
     *
     * @param childEntity
     * @param elderEntity
     * @return
     */
    public boolean isParentEntity(Entity childEntity, Entity elderEntity) {
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

    public LinkedList<Entity> entities(EntitySort order) {
        LinkedList<Entity> e = new LinkedList<>(entities);

        e.sort(
                EntitySort.CHILDREN_FIRST.equals(order)
                        ? new ChildrenFirstComparator(this)
                        : new ChildrenFirstComparator(this).reversed()
        );

        return e;
    }

    public void addEntity(Entity entity) {
        entities.add(entity);
        validated = false;
    }

    @JsonProperty
    public List<biocode.fims.config.models.List> lists() {
        return lists;
    }

    public void addList(biocode.fims.config.models.List list) {
        lists.add(list);
        validated = false;
    }

    @JsonProperty
    public List<ExpeditionMetadataProperty> expeditionMetadataProperties() {
        return expeditionMetadataProperties;
    }

    public void setExpeditionMetadataProperties(List<ExpeditionMetadataProperty> expeditionMetadataProperties) {
        this.expeditionMetadataProperties = expeditionMetadataProperties;
    }

    @JsonIgnore
    public List<String> errors() {
        if (!validated) {
            throw new FimsRuntimeException(ConfigCode.NOT_VALIDATED, 500);
        }
        return (errors != null) ? errors : new ArrayList<>();
    }

    public boolean isEntityChildDescendent(Entity elderEntity, Entity childEntity) {
        return childEntity.isChildEntity() && isParentEntity(childEntity, elderEntity);
    }

    /**
     * get of all entities that form the relation.
     * <p>
     * If the entities are directly related the returned value would be [EntityRelation(elderEntity, childEntity)]
     * If the entities are not directly related the returned list would be [EntityRelation(intermediaryEntity, childEntity), ... , EntityRelation(parentEntity, intermediaryEntity)]
     * If the entities are not related, an empty list will be returned
     *
     * @param primaryEntity
     * @param entity2
     * @return Sorted list of {@link EntityRelation}s in the correct order needed to walk from the primaryEntity to entity2 via a relationship graph
     */
    public List<EntityRelation> getEntityRelations(Entity primaryEntity, Entity entity2) {
        List<EntityRelation> relations = new ArrayList<>();

        if (!entities.contains(primaryEntity) || !entities.contains(entity2)) {
            throw new FimsRuntimeException("Server Error", "Entity doesn't exist", 500);
        }

        ArrayList<Entity> entity2Parents = parentEntities(entity2.getConceptAlias());
        Collections.reverse(entity2Parents);

        List<Entity> primaryEntityParentsToJoin = null;

        boolean found = false;

        if (entity2Parents.contains(primaryEntity)) {
            int i = entity2Parents.indexOf(primaryEntity);
            primaryEntityParentsToJoin = entity2Parents.subList(i, entity2Parents.size());
        }

        Entity prevEntity = primaryEntity;
        for (Entity entity : parentEntities(primaryEntity.getConceptAlias())) {
            relations.add(new EntityRelation(entity, prevEntity));

            int i = entity2Parents.indexOf(entity);
            if (i > -1) {
                // we've found a common ancestor, now set primaryEntityParentsToJoin
                primaryEntityParentsToJoin = entity2Parents.subList(i, entity2Parents.size());
                break;
            } else if (entity.equals(entity2)) {
                found = true;
                break;
            }
            prevEntity = entity;
        }

        if (primaryEntityParentsToJoin != null) {
            prevEntity = primaryEntityParentsToJoin.remove(0);
            // primaryEntityParentsToJoin is reversed & ordered top-down
            for (Entity entity : primaryEntityParentsToJoin) {
                // we've found a common ancestor, now build the relation
                // entity2Parents is reversed & ordered top-down
                relations.add(new EntityRelation(prevEntity, entity));
                prevEntity = entity;

                // we should only need to walk down both lists adding to relations for each entry
                // need to reverse this loop? b/c we want all children of the common ancestor
            }
            relations.add(new EntityRelation(prevEntity, entity2));
            found = true;
        }

        if (!found) return Collections.emptyList();

        return relations;
    }

    /**
     * Get all parent entities for the given entity.
     *
     * @param conceptAlias
     * @return Ordered List of entities from parent -> GrandParent -> GreatGrandParent -> ...
     */
    public ArrayList<Entity> parentEntities(String conceptAlias) {
        ArrayList<Entity> parentEntities = new ArrayList<>();
        Entity entity = entity(conceptAlias);

        while (entity.isChildEntity()) {
            entity = entity(entity.getParentEntity());
            parentEntities.add(entity);
        }

        return parentEntities;
    }

    public void generateUris() {
        for (Entity e : entities) {
            e.generateUris();
        }
    }

    public void addDefaultRules() {
        entities.forEach(e -> e.addDefaultRules(this));
    }

    /**
     * Find the required columns on this sheet
     *
     * @param sheetName
     * @param level
     * @return
     */
    public Set<String> getRequiredColumns(String sheetName, RuleLevel level) {
        return this.entitiesForSheet(sheetName)
                .stream()
                .flatMap(e -> {
                    RequiredValueRule rule = e.getRule(RequiredValueRule.class, level);
                    if (rule == null) return new ArrayList<String>().stream();
                    return rule.columns().stream();
                })
                .collect(Collectors.toSet());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Config)) return false;

        Config config = (Config) o;

        if (!entities.equals(config.entities)) return false;
        if (!lists.equals(config.lists)) return false;
        return expeditionMetadataProperties != null ? expeditionMetadataProperties.equals(config.expeditionMetadataProperties) : config.expeditionMetadataProperties == null;
    }

    @Override
    public int hashCode() {
        int result = entities.hashCode();
        result = 31 * result + lists.hashCode();
        result = 31 * result + (expeditionMetadataProperties != null ? expeditionMetadataProperties.hashCode() : 0);
        return result;
    }

    public void addExpeditionMetadataProperty(ExpeditionMetadataProperty prop) {
        expeditionMetadataProperties.add(prop);
    }

    private static class ChildrenFirstComparator implements Comparator<Entity> {

        private final Config config;

        private ChildrenFirstComparator(Config config) {
            this.config = config;
        }

        @Override
        public int compare(Entity e1, Entity e2) {
            if (e1.isChildEntity() && config.isParentEntity(e1, e2)) return -1;
            if (e2.isChildEntity() && config.isParentEntity(e2, e1)) return 1;
            return 0;
        }
    }
}
