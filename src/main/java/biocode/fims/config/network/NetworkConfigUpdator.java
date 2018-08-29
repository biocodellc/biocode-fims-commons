package biocode.fims.config.network;

import biocode.fims.config.models.Entity;
import biocode.fims.fimsExceptions.FimsRuntimeException;
import biocode.fims.fimsExceptions.errorCodes.ConfigCode;

import java.util.ArrayList;
import java.util.List;

/**
 * Controls the changes allowed to be made to the {@link NetworkConfig}. Tracks any new and/or removed {@link Entity},
 * and safeguards immutable config data.
 *
 * @author rjewing
 */
public class NetworkConfigUpdator {

    private final NetworkConfig updatedConfig;
    private List<Entity> newEntities;
    private List<Entity> removedEntities;

    public NetworkConfigUpdator(NetworkConfig updatedConfig) {
        this.updatedConfig = updatedConfig;
        this.newEntities = new ArrayList<>();
        this.removedEntities = new ArrayList<>();
    }

    public NetworkConfig update(NetworkConfig origConfig) {
        for (Entity e : updatedConfig.entities()) {
            Entity origEntity = origConfig.entity(e.getConceptAlias());

            if (origEntity == null) {
                newEntities.add(e);
            } else {
                preserveImmutableData(e, origEntity);
            }

            e.getRules().forEach(r -> r.setNetworkRule(true));
        }

        checkForRemovedEntities(origConfig);

        return updatedConfig;
    }

    /**
     * parentEntity and uniqueKey can not be changed after entity creation
     * <p>
     * either can attribute uris
     *
     * @param updatedEntity
     * @param origEntity
     */
    private void preserveImmutableData(Entity updatedEntity, Entity origEntity) {
        updatedEntity.setParentEntity(origEntity.getParentEntity());
        updatedEntity.setUniqueKey(origEntity.getUniqueKey());

        updatedEntity
                .getAttributes()
                .parallelStream()
                .forEach(attribute -> {
                    try {
                        String origURI = origEntity.getAttribute(attribute.getColumn()).getUri();
                        attribute.setUri(origURI);
                    } catch (FimsRuntimeException e) {
                        if (e.getErrorCode() == ConfigCode.MISSING_ATTRIBUTE) {
                            if (origEntity.getUniqueKeyURI().equals(attribute.getUri())) {
                                attribute.setColumn(origEntity.getUniqueKey());
                                String origURI = origEntity.getAttribute(attribute.getColumn()).getUri();
                                attribute.setUri(origURI);
                            }
                        } else {
                            throw e;
                        }
                    }
                });
    }

    private void checkForRemovedEntities(NetworkConfig origConfig) {
        for (Entity e : origConfig.entities()) {
            if (updatedConfig.entity(e.getConceptAlias()) == null) {
                removedEntities.add(e);
            }
        }
    }

    public List<Entity> newEntities() {
        return newEntities;
    }

    public List<Entity> removedEntities() {
        return removedEntities;
    }
}
