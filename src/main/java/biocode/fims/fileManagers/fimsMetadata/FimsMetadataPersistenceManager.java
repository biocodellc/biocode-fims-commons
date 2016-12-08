package biocode.fims.fileManagers.fimsMetadata;

import biocode.fims.run.ProcessController;
import org.json.simple.JSONArray;

/**
 * Interface for handling fimsMetadata persistence
 */
public interface FimsMetadataPersistenceManager {

    void upload(ProcessController processController, JSONArray fimsMetadata);

    boolean validate(ProcessController processController);

    String getWebAddress();

    String getGraph();

    JSONArray getDataset(ProcessController processController);
}
