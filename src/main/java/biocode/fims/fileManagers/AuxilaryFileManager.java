package biocode.fims.fileManagers;

import biocode.fims.fileManagers.dataset.Dataset;

/**
 * Interface for FileManagers that depend on {@link biocode.fims.fileManagers.dataset.IDatasetFileManager}
 */
public interface AuxilaryFileManager extends FileManager {

    /**
     * method will always be called. Must only return false if the file is actually invalid.
     * If there is no file to validate, must return true
     *
     * @return
     */
    boolean validate(Dataset dataset);

    void upload(boolean newDataset);
}
