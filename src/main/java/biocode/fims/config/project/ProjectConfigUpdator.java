package biocode.fims.config.project;

import biocode.fims.config.models.Entity;

import java.util.ArrayList;
import java.util.List;

/**
 * Controls the changes allowed to be made to the {@link ProjectConfig}, safeguarding immutable config data.
 *
 * @author rjewing
 */
public class ProjectConfigUpdator {

    private final ProjectConfig updatedConfig;
    private List<Entity> newEntities;

    public ProjectConfigUpdator(ProjectConfig updatedConfig) {
        this.updatedConfig = updatedConfig;
        this.newEntities = new ArrayList<>();
    }

    public ProjectConfig update(ProjectConfig origConfig) {
        for (Entity e : updatedConfig.entities()) {
            Entity origEntity = origConfig.entity(e.getConceptAlias());

            if (origEntity == null) {
                newEntities.add(e);
            } else {
                preserveImmutableData(e, origEntity);
            }
        }

        return updatedConfig;
    }

    /**
     * uniqueKey can not be changed after entity creation
     *
     * @param updatedEntity
     * @param origEntity
     */
    private void preserveImmutableData(Entity updatedEntity, Entity origEntity) {
        updatedEntity.setUniqueKey(origEntity.getUniqueKey());
    }

    public List<Entity> newEntities() {
        return newEntities;
    }
}
