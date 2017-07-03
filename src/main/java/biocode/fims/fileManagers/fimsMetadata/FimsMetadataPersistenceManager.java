package biocode.fims.fileManagers.fimsMetadata;

import biocode.fims.run.ProcessController;
import com.fasterxml.jackson.databind.node.ArrayNode;

import java.io.File;

/**
 * Interface for handling fimsMetadata persistence
 */
public interface FimsMetadataPersistenceManager {

    void upload(ProcessController processController, ArrayNode fimsMetadata, String filename);

    boolean validate(ProcessController processController);

    String writeSourceFile(File sourceFile, int projectId, String expeditionCode);

    String getWebAddress();

    String getGraph();

    ArrayNode getDataset(ProcessController processController);

    void deleteDataset(ProcessController processController);
}
