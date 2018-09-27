package biocode.fims.repositories;

import biocode.fims.config.models.Entity;
import biocode.fims.config.network.NetworkConfig;

import java.util.List;

/**
 * @author rjewing
 */
public interface NetworkConfigRepository {

    void save(NetworkConfig config, int networkId);

    void createNetworkSchema(int networkId);

    NetworkConfig getConfig(int networkId);

    void createEntityTables(List<Entity> entities, int networkId, NetworkConfig config);

    void removeEntityTables(List<Entity> entities, int networkId);
}
