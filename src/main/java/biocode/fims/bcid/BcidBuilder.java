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

    public BcidBuilder(Entity entity) {
        this(entity, null);
    }

    public BcidBuilder(Entity entity, Entity parentEntity) {
        if (entity.isChildEntity() && parentEntity == null) {
            throw new IllegalArgumentException("parentEntity is required if entity is a childEntity");
        }
        this.entity = entity;
        this.parentEntity = parentEntity;
    }

    public String build(Record record) {
        String bcid = record.rootIdentifier();
        if (entity.isChildEntity() && entity.getUniqueKey() != null) {
            bcid += record.get(entity.getUniqueKeyURI());
        } else if (entity.isChildEntity()) {
            bcid += record.get(parentEntity.getUniqueKeyURI());
        } else {
            bcid += record.get(entity.getUniqueKeyURI());
        }

        return bcid;
    }
}
