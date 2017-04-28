package biocode.fims.repositories.customOperations;

import biocode.fims.models.Expedition;

import java.util.List;

/**
 * defines custom ExpeditionRepository operations
 */
public interface ExpeditionCustomOperations {
    void save(List<Expedition> expeditions);
}
