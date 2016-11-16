package biocode.fims.fileManagers.dataset;

import biocode.fims.fileManagers.FileManager;
import org.json.simple.JSONArray;

/**
 * Interface for FileManagers handling datasets
 */
public interface IDatasetFileManager extends FileManager {

    /**
     * method will always be called. Must only return false if the file is actually invalid.
     * If there is no file to validate, must return true
     *
     * @return
     */
    boolean validate();

    void upload();

    /**
     * if we are uploading a new dataset
     * @return
     */
    boolean isNewDataset();

    Dataset getDataset();
}