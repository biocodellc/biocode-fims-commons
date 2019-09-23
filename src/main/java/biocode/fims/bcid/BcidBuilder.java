package biocode.fims.bcid;

import biocode.fims.config.models.Entity;
import biocode.fims.records.Record;

/**
 * Helper class to build a bcid for a {@link Record}
 *
 * @author rjewing
 */
public class BcidBuilder {

    private Entity entity;
    private Entity parentEntity;
    private String resolverPrefix;

    public BcidBuilder(Entity entity, Entity parentEntity, String resolverPrefix) {
        this.resolverPrefix = resolverPrefix;
        if (entity.isChildEntity() && entity.getUniqueKey() == null && parentEntity == null) {
            throw new IllegalArgumentException("parentEntity is required if entity is a childEntity and uniqueKey is null");
        }
        this.entity = entity;
        this.parentEntity = parentEntity;
    }

    public String build(Record record) {
        String bcid = resolverPrefix + record.rootIdentifier();
        if (entity.isChildEntity() && entity.getUniqueKey() == null) {
            bcid += record.get(parentEntity.getUniqueKeyURI());
        } else {
            bcid += record.get(entity.getUniqueKeyURI());
        }

        return bcid;
    }
}
