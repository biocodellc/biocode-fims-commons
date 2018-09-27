package biocode.fims.service;

import biocode.fims.config.network.NetworkConfig;
import biocode.fims.config.network.NetworkConfigUpdator;
import biocode.fims.fimsExceptions.FimsRuntimeException;
import biocode.fims.fimsExceptions.ForbiddenRequestException;
import biocode.fims.fimsExceptions.errorCodes.ConfigCode;
import biocode.fims.models.*;
import biocode.fims.repositories.NetworkConfigRepository;
import biocode.fims.repositories.NetworkRepository;
import biocode.fims.repositories.SetFimsUser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;


/**
 * Service class for handling {@link Network} persistence
 */
@Service
@Transactional
public class NetworkService {

    private final NetworkRepository networkRepository;
    private final NetworkConfigRepository networkConfigRepository;

    @Autowired
    public NetworkService(NetworkRepository networkRepository, NetworkConfigRepository networkConfigRepository) {
        this.networkRepository = networkRepository;
        this.networkConfigRepository = networkConfigRepository;
    }

    @SetFimsUser
    public Network update(Network network) {
        Network existingNetwork = getNetwork(network.getId());

        if (existingNetwork == null) {
            throw new FimsRuntimeException("network not found", 404);
        }

        if (network.getUser() == null || !network.getUser().equals(existingNetwork.getUser())) {
            throw new ForbiddenRequestException("You must be this network's admin in order to update the metadata");
        }

        updateExistingNetwork(existingNetwork, network);
        saveConfig(network.getNetworkConfig(), network.getId());
        networkRepository.save(network);
        return network;
    }

    /**
     * method to transfer the updated {@link Network} object to an existing {@link Network}. This
     * allows us to control which properties can be updated.
     * Currently allows updating of the following properties : description, title
     *
     * @param existingNetwork
     * @param updatedNetwork
     */
    private void updateExistingNetwork(Network existingNetwork, Network updatedNetwork) {
        existingNetwork.setTitle(updatedNetwork.getTitle());
        existingNetwork.setDescription(updatedNetwork.getDescription());
    }


    public Network getNetwork(int id) {
        return networkRepository.findById(id);
    }

    public void createNetworkSchema(int networkId) {
        networkConfigRepository.createNetworkSchema(networkId);
    }

    public void saveConfig(NetworkConfig config, int networkId) {
        config.generateUris();
        config.addDefaultRules();

        if (!config.isValid()) {
            throw new FimsRuntimeException(ConfigCode.INVALID, 400);
        }

        NetworkConfig existingConfig = networkConfigRepository.getConfig(networkId);

        if (existingConfig == null) {
            existingConfig = new NetworkConfig();
        }

        NetworkConfigUpdator updator = new NetworkConfigUpdator(config);
        config = updator.update(existingConfig);

        if (updator.newEntities().size() > 0) {
            networkConfigRepository.createEntityTables(updator.newEntities(), networkId, config);
        }

        if (updator.removedEntities().size() > 0) {
            networkConfigRepository.removeEntityTables(updator.removedEntities(), networkId);
        }

        networkConfigRepository.save(config, networkId);
    }

    public List<Network> getNetworks() {
        return networkRepository.findAll();
    }

    public boolean isNetworkAdmin(User user, int networkId) {
        if (user == null) {
            return false;
        }

        Network network = networkRepository.findById(networkId);

        if (network == null) {
            throw new FimsRuntimeException("network not found", 404);
        }

        return network.getUser().equals(user);
    }
}


