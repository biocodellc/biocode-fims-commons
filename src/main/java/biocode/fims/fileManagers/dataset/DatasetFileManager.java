package biocode.fims.fileManagers.dataset;

import biocode.fims.bcid.ResourceTypes;
import biocode.fims.digester.Mapping;
import biocode.fims.digester.Validation;
import biocode.fims.entities.Bcid;
import biocode.fims.entities.Expedition;
import biocode.fims.fimsExceptions.FimsRuntimeException;
import biocode.fims.reader.DatasetTabularDataConverter;
import biocode.fims.reader.ReaderManager;
import biocode.fims.reader.plugins.TabularDataReader;
import biocode.fims.renderers.RowMessage;
import biocode.fims.run.ProcessController;
import biocode.fims.service.BcidService;
import biocode.fims.service.ExpeditionService;
import biocode.fims.settings.SettingsManager;
import biocode.fims.tools.ServerSideSpreadsheetTools;
import biocode.fims.utils.FileUtils;
import org.json.simple.JSONArray;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.Assert;

import java.io.File;
import java.net.URI;

/**
 * IDatasetFileManger implementation to handle datasets
 */
public class DatasetFileManager implements IDatasetFileManager {
    private static final String NAME = "dataset";

    private final DatasetPersistenceManager persistenceManager;
    private final SettingsManager settingsManager;
    private final ExpeditionService expeditionService;
    private final BcidService bcidService;
    private ProcessController processController;
    private String filename;
    private Dataset dataset;

    @Autowired
    public DatasetFileManager(DatasetPersistenceManager persistenceManager, SettingsManager settingsManager,
                              ExpeditionService expeditionService, BcidService bcidService) {
        this.persistenceManager = persistenceManager;
        this.settingsManager = settingsManager;
        this.expeditionService = expeditionService;
        this.bcidService = bcidService;
    }

    @Override
    public void upload() {
        Assert.notNull(processController);

        if (filename != null) {
            persistenceManager.upload(processController, dataset);

            Bcid bcid = new Bcid.BcidBuilder(ResourceTypes.DATASET_RESOURCE_TYPE)
                    .ezidRequest(Boolean.parseBoolean(settingsManager.retrieveValue("ezidRequests")))
                    .title("Dataset: " + processController.getExpeditionCode())
                    .webAddress(URI.create(persistenceManager.getWebAddress()))
                    .graph(persistenceManager.getGraph())
                    .finalCopy(processController.getFinalCopy())
                    .build();

            bcidService.create(bcid, processController.getUserId());

            Expedition expedition = expeditionService.getExpedition(
                    processController.getExpeditionCode(),
                    processController.getProjectId()
            );

            bcidService.attachBcidToExpedition(
                    bcid,
                    expedition.getExpeditionId()
            );

            // save the spreadsheet on the server
            File inputFile = new File(filename);
            String ext = FileUtils.getExtension(inputFile.getName(), null);
            String filename = "bcid_id_" + bcid.getBcidId() + "." + ext;
            File outputFile = new File(settingsManager.retrieveValue("serverRoot") + filename);

            ServerSideSpreadsheetTools serverSideSpreadsheetTools = new ServerSideSpreadsheetTools(inputFile);
            serverSideSpreadsheetTools.write(outputFile);

            bcid.setSourceFile(filename);
            bcidService.update(bcid);

            processController.appendSuccessMessage(
                    "Dataset Identifier: http://n2t.net/" + bcid.getIdentifier() +
                            " (wait 15 minutes for resolution to become active)" +
                            "<br>\t" + "Data Elements Root: " + processController.getExpeditionCode()
            );
        }
    }

    @Override
    public boolean isNewDataset() {
        return filename != null;
    }

    @Override
    public boolean validate() {
        Assert.notNull(processController);

        if (filename != null) {
            processController.appendStatus("\nRunning dataset validation.");
            Mapping mapping = processController.getMapping();
            Validation validation = processController.getValidation();
            String outputPrefix = processController.getExpeditionCode() + "_output";

            String sheetName = mapping.getDefaultSheetName();
            // Create the tabularDataReader for reading the input file
            ReaderManager rm = new ReaderManager();
            rm.loadReaders();
            TabularDataReader tdr = rm.openFile(filename, sheetName, processController.getOutputFolder());

            if (tdr == null) {
                processController.appendStatus("<br>Unable to open the file you attempted to upload.<br>");
                return false;
            }

            try {
                DatasetTabularDataConverter tdc = new DatasetTabularDataConverter(tdr);
                dataset = tdc.convert(mapping.getAllAttributes(sheetName), sheetName);

                // Run the validation
                validation.run(tdr, outputPrefix, processController.getOutputFolder(), mapping, dataset);
            } catch (FimsRuntimeException e) {
                if (e.getErrorCode() != null) {
                    processController.addMessage(sheetName, new RowMessage(e.getUsrMessage(), "Initial Spreadsheet check", RowMessage.ERROR));
                    return false;
                } else {
                    throw e;
                }
            } finally {
                tdr.closeFile();
            }

            // get the Messages from each worksheet and add them to the processController
            processController.addMessages(validation.getMessages());

            if (validation.hasErrors()) {
                return false;
            } else if (validation.hasWarnings()) {
                processController.setHasWarnings(true);
            }
        }
        return true;
    }

    @Override
    public void setProcessController(ProcessController processController) {
        this.processController = processController;
    }

    @Override
    public Dataset getDataset() {
        if (dataset == null) {
            dataset = persistenceManager.getDataset(processController);
        }

        return dataset;
    }

    @Override
    public void setFilename(String filename) {
        if (this.filename != null) {
            throw new FimsRuntimeException("Server Error", "You can only upload 1 dataset at a time", 500);
        }
        this.filename = filename;
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public void close() {
        if (filename != null) {
            new File(filename).delete();
        }
    }
}
