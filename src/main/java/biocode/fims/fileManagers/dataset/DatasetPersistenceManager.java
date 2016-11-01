package biocode.fims.fileManagers.dataset;

import biocode.fims.run.ProcessController;
import org.json.simple.JSONArray;

/**
 * Interface for handling dataset persistence
 */
public interface DatasetPersistenceManager {

    void upload(ProcessController processController, Dataset dataset);

    boolean validate(ProcessController processController);

    String getWebAddress();

    String getGraph();

    Dataset getDataset(ProcessController processController);
}
