package biocode.fims.query;

import biocode.fims.digester.*;
import biocode.fims.fimsExceptions.FimsRuntimeException;
import biocode.fims.fimsExceptions.errorCodes.QueryCode;
import biocode.fims.projectConfig.ProjectConfig;

import java.util.*;
import java.util.List;

/**
 * Used to generate a SQL JOIN statement for queries on {@link Entity} queryEntity and any related entities.
 * <p>
 * If no entities are added, an empty string will be built. Otherwise, the appropriate JOIN string will be
 * built, including intermediary {@link Entity} for relationships that span multiple entities
 * (ex. joining parentEntity and gradChildEntity, w/o explicitly adding childEntity to the JoinBuilder)
 * <p>
 * A {@link FimsRuntimeException} with {@link QueryCode#UNRELATED_ENTITIES} will be thrown during build() if
 * any {@link Entity} is added that is not related to the queryEntity.
 *
 * @author rjewing
 */
public class JoinBuilder {

    private final Set<Entity> joinEntities;
    private final Entity queryEntity;
    private final ProjectConfig config;
    private final int projectId;
    private final List<Entity> joinedEntities;
    private final StringBuilder joinString;
    private boolean expeditions;

    JoinBuilder(Entity queryEntity, ProjectConfig config, int projectId) {
        this.queryEntity = queryEntity;
        this.config = config;
        this.projectId = projectId;
        this.joinEntities = new HashSet<>();
        this.joinedEntities = new ArrayList<>();
        this.joinString = new StringBuilder();
        this.expeditions = false;
    }

    public void add(Entity entity) {
        joinEntities.add(entity);
    }

    void joinExpeditions(boolean val) {
        expeditions = val;
    }

    public String build() {
        if (expeditions) {
            appendExpeditionsJoin();
        }

        buildJoinEntities();

        return joinString.toString();
    }

    private void appendExpeditionsJoin() {
        joinString
                .append(" JOIN expeditions ON expeditions.id = ")
                .append(queryEntity.getConceptAlias())
                .append(".expedition_id");
    }

    private void buildJoinEntities() {
        for (Entity entity : joinEntities) {
            verifyRelated(entity);

            if (config.isEntityChildDescendent(queryEntity, entity)) {
                buildChildJoin(entity);
            } else {
                buildParentJoin(entity);
            }
        }
    }

    private void verifyRelated(Entity entity) {
        if (!config.areRelatedEntities(queryEntity.getConceptAlias(), entity.getConceptAlias())) {
            throw new FimsRuntimeException(QueryCode.UNRELATED_ENTITIES, 400, entity.getConceptAlias(), queryEntity.getConceptAlias());
        }
    }

    private void buildChildJoin(Entity entity) {
        LinkedList<Entity> entityHierarchy = config.entitiesInRelation(queryEntity, entity);
        entityHierarchy.remove(queryEntity);

        Entity parentEntity = queryEntity;

        for (Entity childEntity : entityHierarchy) {

            if (alreadyJoined(childEntity)) {
                parentEntity = childEntity;
                continue;
            }

            appendChildJoin(parentEntity, childEntity);

            joinedEntities.add(childEntity);
            parentEntity = childEntity;
        }
    }

    private void buildParentJoin(Entity entity) {
        LinkedList<Entity> entityHierarchy = config.entitiesInRelation(entity, queryEntity);
        entityHierarchy.remove(queryEntity);
        Collections.reverse(entityHierarchy);

        Entity childEntity = queryEntity;

        for (Entity parentEntity : entityHierarchy) {

            if (alreadyJoined(parentEntity)) {
                childEntity = parentEntity;
                continue;
            }

            appendParentJoin(childEntity, parentEntity);

            joinedEntities.add(parentEntity);
            childEntity = parentEntity;
        }
    }

    private void appendChildJoin(Entity parentEntity, Entity childEntity) {
        String joinColumn = getJoinColumn(parentEntity, childEntity);
        String table = PostgresUtils.entityTableAs(projectId, childEntity.getConceptAlias());

        joinString
                .append(" JOIN ")
                .append(table)
                .append(" ON ")
                .append(childEntity.getConceptAlias())
                .append(".data->>'")
                .append(joinColumn)
                .append("' = ")
                .append(parentEntity.getConceptAlias())
                .append(".local_identifier")
                .append(" and ")
                .append(childEntity.getConceptAlias())
                .append(".expedition_id = ")
                .append(parentEntity.getConceptAlias())
                .append(".expedition_id");
    }

    private void appendParentJoin(Entity childEntity, Entity parentEntity) {
        String joinColumn = getJoinColumn(parentEntity, childEntity);
        String table = PostgresUtils.entityTableAs(projectId, parentEntity.getConceptAlias());

        joinString
                .append(" JOIN ")
                .append(table)
                .append(" ON ")
                .append(parentEntity.getConceptAlias())
                .append(".local_identifier = ")
                .append(childEntity.getConceptAlias())
                .append(".data->>'")
                .append(joinColumn)
                .append("' and ")
                .append(parentEntity.getConceptAlias())
                .append(".expedition_id = ")
                .append(childEntity.getConceptAlias())
                .append(".expedition_id");
    }

    private boolean alreadyJoined(Entity joinEntity) {
        return joinedEntities.contains(joinEntity);
    }

    private String getJoinColumn(Entity parentEntity, Entity childEntity) {
        return childEntity.getAttributeUri(parentEntity.getUniqueKey());
    }
}