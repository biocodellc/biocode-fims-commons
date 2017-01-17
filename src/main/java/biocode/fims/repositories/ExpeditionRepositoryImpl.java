package biocode.fims.repositories;

import biocode.fims.entities.Expedition;
import biocode.fims.entities.Project;
import biocode.fims.repositories.customOperations.ExpeditionCustomOperations;
import biocode.fims.repositories.customOperations.ProjectCustomOperations;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.util.List;

/**
 * Implementation of ProjectCustomOperations
 */
public class ExpeditionRepositoryImpl implements ExpeditionCustomOperations {
    private static int BATCH_SIZE = 100;
    @PersistenceContext(unitName = "entityManagerFactory")
    private EntityManager em;

    @Transactional
    @Override
    public void save(List<Expedition> expeditions) {
        int i = 0;
        for (Expedition expedition: expeditions) {
            em.merge(expedition);
            i++;

            if (i % BATCH_SIZE == 0) {
                em.flush();
                em.clear();
            }
        }

    }
}
