package biocode.fims.fileManagers.fimsMetadata;

import biocode.fims.settings.SettingsManager;
import biocode.fims.tools.ServerSideSpreadsheetTools;
import biocode.fims.utils.FileUtils;

import java.io.File;

/**
 * Abstract FimsMetadataPersistenceManager providing common code for {@link FimsMetadataPersistenceManager} implementations
 * @author RJ Ewing
 */
public abstract class AbstractFimsMetadataPersistenceManager implements FimsMetadataPersistenceManager {
    private final SettingsManager settingsManager;

    protected AbstractFimsMetadataPersistenceManager(SettingsManager settingsManager) {
        this.settingsManager = settingsManager;
    }

    public String writeSourceFile(File sourceFile, int bcidId) {
        String ext = FileUtils.getExtension(sourceFile.getName(), null);
        String filename = "fims_metadata_bcid_id_" + bcidId + "." + ext;
        File outputFile = new File(settingsManager.retrieveValue("serverRoot") + filename);

        ServerSideSpreadsheetTools serverSideSpreadsheetTools = new ServerSideSpreadsheetTools(sourceFile);
        serverSideSpreadsheetTools.write(outputFile);
        return outputFile.getName();
    }


}
