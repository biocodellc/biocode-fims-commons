package biocode.fims.fileManagers;

import com.fasterxml.jackson.databind.node.ArrayNode;

/**
 * Interface for FileManagers that depend on {@link biocode.fims.fileManagers.fimsMetadata.FimsMetadataFileManager}
 */
public interface AuxilaryFileManager extends FileManager {

    /**
     * method will always be called. Must only return false if the file is actually invalid.
     * If there is no file to validate, must return true
     *
     * @return
     */
    boolean validate(ArrayNode fimsMetadata);

    void upload(boolean newDataset);

    /**
     * method will always be called. this is called to add any additional data to the Dataset for indexing via ElasticSearch.
     **/
    void index(ArrayNode dataset);
}
