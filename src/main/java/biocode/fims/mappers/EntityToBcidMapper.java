package biocode.fims.mappers;

import biocode.fims.digester.Entity;
import biocode.fims.entities.Bcid;

/**
 * Class to map {@link Entity} to {@link Bcid}
 */
public class EntityToBcidMapper {

    public static Bcid map(Entity entity, boolean ezidRequest) {
        return new Bcid.BcidBuilder(entity.getConceptURI())
                .title(entity.getConceptAlias())
                .ezidRequest(ezidRequest)
                .build();
    }
}
