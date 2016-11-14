package biocode.fims.fileManagers.dataset;

import biocode.fims.run.ProcessController;
import org.json.simple.JSONArray;

/**
 * Interface for handling dataset persistence
 */
public interface DatasetPersistenceManager {

    void upload(ProcessController processController, JSONArray dataset);

    boolean validate(ProcessController processController);

    String getWebAddress();

    String getGraph();

    JSONArray getDataset(ProcessController processController);
}
