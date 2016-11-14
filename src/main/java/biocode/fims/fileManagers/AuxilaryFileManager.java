package biocode.fims.fileManagers;

import biocode.fims.fileManagers.dataset.DatasetFileManager;
import org.json.simple.JSONArray;

/**
 * Interface for FileManagers that depend on {@link DatasetFileManager}
 */
public interface AuxilaryFileManager extends FileManager {

    /**
     * method will always be called. Must only return false if the file is actually invalid.
     * If there is no file to validate, must return true
     *
     * @return
     */
    boolean validate(JSONArray dataset);

    void upload(boolean newDataset);
}
