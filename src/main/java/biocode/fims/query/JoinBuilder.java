package biocode.fims.query;

import biocode.fims.config.Config;
import biocode.fims.config.project.ProjectConfig;
import biocode.fims.fimsExceptions.FimsRuntimeException;
import biocode.fims.fimsExceptions.errorCodes.QueryCode;
import biocode.fims.config.models.Entity;
import biocode.fims.models.EntityRelation;

import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

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
    private final Set<Entity> selectEntities;
    private final int networkId;
    private final List<Entity> joinedEntities;
    private final StringBuilder joinString;
    private Entity queryEntity;
    private Config config;
    private boolean expeditions = false;

    JoinBuilder(Entity queryEntity, Config config, int networkId) {
        this.queryEntity = queryEntity;
        this.config = config;
        this.networkId = networkId;
        this.joinEntities = new HashSet<>();
        this.selectEntities = new HashSet<>();
        this.joinedEntities = new ArrayList<>();
        this.joinString = new StringBuilder();
    }

    public void add(Entity entity) {
        joinEntities.add(entity);
    }

    public void addSelect(Entity entity) {
        if (queryEntity.equals(entity)) return;
        add(entity);
        selectEntities.add(entity);
    }

    public List<Entity> selectEntities() {
        return new ArrayList<>(selectEntities);
    }

    void joinExpeditions(boolean val) {
        expeditions = val;
    }

    public String build() {
        if (expeditions) {
            appendExpeditionsJoin();
        }

        buildJoinEntities();
        appendEntityIdentifiersJoin();

        return joinString.toString();
    }

    private void appendExpeditionsJoin() {
        joinString
                .append(" JOIN expeditions ON expeditions.id = ")
                .append(queryEntity.getConceptAlias())
                .append(".expedition_id");
    }

    private void appendEntityIdentifiersJoin() {
        joinString
                .append(" LEFT JOIN entity_identifiers AS ")
                .append(queryEntity.getConceptAlias())
                .append("_entity_identifiers ON ")
                .append(queryEntity.getConceptAlias())
                .append("_entity_identifiers.expedition_id = ")
                .append(queryEntity.getConceptAlias())
                .append(".expedition_id and ")
                .append(queryEntity.getConceptAlias())
                .append("_entity_identifiers.concept_alias = '")
                .append(queryEntity.getConceptAlias())
                .append("'");

        for (Entity e : selectEntities) {
            verifyRelated(e);
            joinString
                    .append(" LEFT JOIN entity_identifiers AS ")
                    .append(e.getConceptAlias())
                    .append("_entity_identifiers ON ")
                    .append(e.getConceptAlias())
                    .append("_entity_identifiers.expedition_id = ")
                    .append(e.getConceptAlias())
                    .append(".expedition_id and ")
                    .append(e.getConceptAlias())
                    .append("_entity_identifiers.concept_alias = '")
                    .append(e.getConceptAlias())
                    .append("'");
        }
    }

    private void buildJoinEntities() {
        // sorting provides deterministic behavior so we can correctly test
        ArrayList<Entity> sortedJoinEntities = new ArrayList<>(joinEntities);
        sortedJoinEntities.sort(Comparator.comparing(Entity::getConceptAlias));
        for (Entity entity : sortedJoinEntities) {
            verifyRelated(entity);

            buildJoin(entity);
        }
    }

    private void verifyRelated(Entity entity) {
        if (!config.areRelatedEntities(queryEntity.getConceptAlias(), entity.getConceptAlias())) {
            throw new FimsRuntimeException(QueryCode.UNRELATED_ENTITIES, 400, entity.getConceptAlias(), queryEntity.getConceptAlias());
        }
    }

    private void buildJoin(Entity entity) {
        Entity prevEntity = queryEntity;

        // TODO: This logic may break when we temporarily step down a relationship graph
        // and then continue traversing up. I'm not sure if we will ever run into this
        // sort of edge case.
        for (EntityRelation relation : config.getEntityRelations(queryEntity, entity)) {

            if (relation.getChildEntity().equals(prevEntity)) {
                prevEntity = relation.getParentEntity();
                if (alreadyJoined(prevEntity)) continue;

                appendParentJoin(relation.getChildEntity(), relation.getParentEntity());
            } else {
                prevEntity = relation.getChildEntity();
                if (alreadyJoined(prevEntity)) continue;

                appendChildJoin(relation.getParentEntity(), relation.getChildEntity());
            }

            joinedEntities.add(prevEntity);
        }
    }


    private void appendChildJoin(Entity parentEntity, Entity childEntity) {
        String table = PostgresUtils.entityTableAs(networkId, childEntity.getConceptAlias());

        joinString
                .append(" LEFT JOIN ")
                .append(table)
                .append(" ON ")
                .append(childEntity.getConceptAlias())
                .append(".parent_identifier = ")
                .append(parentEntity.getConceptAlias())
                .append(".local_identifier")
                .append(" and ")
                .append(childEntity.getConceptAlias())
                .append(".expedition_id = ")
                .append(parentEntity.getConceptAlias())
                .append(".expedition_id");
    }

    private void appendParentJoin(Entity childEntity, Entity parentEntity) {
        String table = PostgresUtils.entityTableAs(networkId, parentEntity.getConceptAlias());

        joinString
                .append(" JOIN ")
                .append(table)
                .append(" ON ")
                .append(parentEntity.getConceptAlias())
                .append(".local_identifier = ")
                .append(childEntity.getConceptAlias())
                .append(".parent_identifier and ")
                .append(parentEntity.getConceptAlias())
                .append(".expedition_id = ")
                .append(childEntity.getConceptAlias())
                .append(".expedition_id");
    }

    private boolean alreadyJoined(Entity joinEntity) {
        return joinedEntities.contains(joinEntity) || queryEntity.equals(joinEntity);
    }

    public void setProjectConfig(ProjectConfig config, Entity queryEntity) {
        this.config = config;
        this.queryEntity = queryEntity;
    }
}
