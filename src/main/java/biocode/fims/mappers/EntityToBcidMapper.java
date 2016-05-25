package biocode.fims.mappers;

import biocode.fims.digester.Entity;
import biocode.fims.entities.Bcid;

/**
 * Class to map {@link Entity} to {@link Bcid}
 */
public class EntityToBcidMapper {

    public Bcid map(Entity entity) {
        return new Bcid.BcidBuilder(entity.getConceptURI())
                .title(entity.getConceptAlias())
                .build();
    }
}
