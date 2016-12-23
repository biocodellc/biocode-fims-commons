package biocode.fims.fileManagers.fimsMetadata;

import biocode.fims.entities.Bcid;
import biocode.fims.run.ProcessController;
import org.json.simple.JSONArray;

import java.io.File;

/**
 * Interface for handling fimsMetadata persistence
 */
public interface FimsMetadataPersistenceManager {

    void upload(ProcessController processController, JSONArray fimsMetadata, String filename);

    boolean validate(ProcessController processController);

    String writeSourceFile(File sourceFile, int bcidId);

    String getWebAddress();

    String getGraph();

    JSONArray getDataset(ProcessController processController);
}
