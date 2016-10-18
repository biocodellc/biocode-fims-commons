package biocode.fims.fileManagers;

import biocode.fims.run.ProcessController;

/**
 * interface for FileManagers
 */
public interface FileManager {

    String getName();
    void setFilename(String value);
    void setProcessController(ProcessController processController);
    void close();
}
