package biocode.fims.mappers;

import biocode.fims.digester.Entity;
import biocode.fims.entities.BcidTmp;

/**
 * Class to map {@link Entity} to {@link BcidTmp}
 */
public class EntityToBcidMapper {

    public static BcidTmp map(Entity entity, boolean ezidRequest) {
        return new BcidTmp.BcidBuilder(entity.getConceptURI())
                .title(entity.getConceptAlias())
                .ezidRequest(ezidRequest)
                .build();
    }
}
