package biocode.fims.repositories;

import biocode.fims.models.Network;
import org.springframework.data.repository.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * This repositories provides CRUD operations for {@link Network} objects
 */
@Transactional
public interface NetworkRepository extends Repository<Network, Integer> {

    void save(Network network);

    Network findById(int id);

    List<Network> findAll();
}
