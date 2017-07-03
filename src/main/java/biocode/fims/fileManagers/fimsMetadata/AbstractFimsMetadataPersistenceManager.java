package biocode.fims.fileManagers.fimsMetadata;

import biocode.fims.application.config.FimsProperties;
import biocode.fims.fimsExceptions.FimsRuntimeException;
import biocode.fims.fimsExceptions.errorCodes.FileCode;
import biocode.fims.utils.FileUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Date;

/**
 * Abstract FimsMetadataPersistenceManager providing common code for {@link FimsMetadataPersistenceManager} implementations
 * @author RJ Ewing
 */
public abstract class AbstractFimsMetadataPersistenceManager implements FimsMetadataPersistenceManager {
    private final FimsProperties props;

    protected AbstractFimsMetadataPersistenceManager(FimsProperties props) {
        this.props = props;
    }

    public String writeSourceFile(File sourceFile, int projectId, String expeditionCode) {
        String ext = FileUtils.getExtension(sourceFile.getName(), null);
        String filename = "fims_metadata_project_" + projectId + "_expedition_" + expeditionCode + "_" + new Date().getTime() + "." + ext;
        File outputFile = new File(props.serverRoot() + filename);

        try {
            Files.copy(sourceFile.toPath(), outputFile.toPath());
        } catch (IOException e) {
            throw new FimsRuntimeException(FileCode.WRITE_ERROR, 500);
        }

        return outputFile.getName();
    }


}
