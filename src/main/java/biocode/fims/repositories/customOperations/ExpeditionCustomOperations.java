package biocode.fims.repositories.customOperations;

import biocode.fims.entities.Expedition;

import java.util.List;

/**
 * defines custom ExpeditionRepository operations
 */
public interface ExpeditionCustomOperations {
    void save(List<Expedition> expeditions);
}
