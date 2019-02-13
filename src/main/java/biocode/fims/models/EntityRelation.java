package biocode.fims.models;

import biocode.fims.config.models.Entity;

/**
 * @author rjewing
 */
public class EntityRelation {
    private final Entity parentEntity;
    private final Entity childEntity;

    public EntityRelation(Entity parentEntity, Entity childEntity) {
        this.parentEntity = parentEntity;
        this.childEntity = childEntity;
    }

    public Entity getParentEntity() {
        return parentEntity;
    }

    public Entity getChildEntity() {
        return childEntity;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof EntityRelation)) return false;

        EntityRelation that = (EntityRelation) o;

        if (getParentEntity() != null ? !getParentEntity().equals(that.getParentEntity()) : that.getParentEntity() != null)
            return false;
        return getChildEntity() != null ? getChildEntity().equals(that.getChildEntity()) : that.getChildEntity() == null;
    }

    @Override
    public int hashCode() {
        int result = getParentEntity() != null ? getParentEntity().hashCode() : 0;
        result = 31 * result + (getChildEntity() != null ? getChildEntity().hashCode() : 0);
        return result;
    }
}
