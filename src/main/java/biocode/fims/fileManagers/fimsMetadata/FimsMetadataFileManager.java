package biocode.fims.fileManagers.fimsMetadata;

import biocode.fims.bcid.ResourceTypes;
import biocode.fims.digester.Attribute;
import biocode.fims.digester.Entity;
import biocode.fims.digester.Mapping;
import biocode.fims.digester.Validation;
import biocode.fims.entities.Bcid;
import biocode.fims.entities.Expedition;
import biocode.fims.fileManagers.FileManager;
import biocode.fims.fimsExceptions.FimsRuntimeException;
import biocode.fims.reader.JsonTabularDataConverter;
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
import org.json.simple.JSONObject;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;

import java.io.File;
import java.net.URI;
import java.util.*;

/**
 * special FileManger implementation to handle FimsMetadata files
 */
public class FimsMetadataFileManager implements FileManager {
    public static final String NAME = "fimsMetadata";
    public static final String DATASET_RESOURCE_SUB_TYPE = "FimsMetadata";

    private final FimsMetadataPersistenceManager persistenceManager;
    private final SettingsManager settingsManager;
    private final ExpeditionService expeditionService;
    private final BcidService bcidService;
    private ProcessController processController;
    private String filename;
    private JSONArray fimsMetadata;

    public FimsMetadataFileManager(FimsMetadataPersistenceManager persistenceManager, SettingsManager settingsManager,
                                   ExpeditionService expeditionService, BcidService bcidService) {
        this.persistenceManager = persistenceManager;
        this.settingsManager = settingsManager;
        this.expeditionService = expeditionService;
        this.bcidService = bcidService;
    }

    public void upload() {
        Assert.notNull(processController);

        if (filename != null) {
            persistenceManager.upload(processController, fimsMetadata);

            URI webaddress = persistenceManager.getWebAddress() != null ? URI.create(persistenceManager.getWebAddress()) : null;

            Bcid bcid = new Bcid.BcidBuilder(ResourceTypes.DATASET_RESOURCE_TYPE)
                    .ezidRequest(Boolean.parseBoolean(settingsManager.retrieveValue("ezidRequests")))
                    .title("Fims Metadata Dataset: " + processController.getExpeditionCode())
                    .webAddress(webaddress)
                    .graph(persistenceManager.getGraph())
                    .subResourceType(DATASET_RESOURCE_SUB_TYPE)
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
            String filename = "fims_metadata_bcid_id_" + bcid.getBcidId() + "." + ext;
            File outputFile = new File(settingsManager.retrieveValue("serverRoot") + filename);

            ServerSideSpreadsheetTools serverSideSpreadsheetTools = new ServerSideSpreadsheetTools(inputFile);
            serverSideSpreadsheetTools.write(outputFile);

            bcid.setSourceFile(filename);
            bcidService.update(bcid);

            processController.appendSuccessMessage(
                    "Fims Metadata Dataset Identifier: http://n2t.net/" + bcid.getIdentifier() +
                            " (wait 15 minutes for resolution to become active)" +
                            "<br>\t" + "Data Elements Root: " + processController.getExpeditionCode()
            );
        }
    }

    public boolean isNewDataset() {
        return filename != null;
    }

    public boolean validate() {
        Assert.notNull(processController);

        if (filename != null) {
            processController.appendStatus("\nRunning fims metadata dataset validation.");
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
                JsonTabularDataConverter tdc = new JsonTabularDataConverter(tdr);
                fimsMetadata = tdc.convert(mapping.getDefaultSheetAttributes(), sheetName);

                // Run the validation
                validation.run(tdr, outputPrefix, processController.getOutputFolder(), mapping, fimsMetadata);
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

            if (validation.hasErrors() || !persistenceManager.validate(processController)) {
                return false;
            } else if (validation.hasWarnings()) {
                processController.setHasWarnings(true);
            }
        }
        return true;
    }

    public void setProcessController(ProcessController processController) {
        this.processController = processController;
    }

    public JSONArray getDataset() {
        if (fimsMetadata == null) {
            fimsMetadata = persistenceManager.getDataset(processController);
        }

        return fimsMetadata;
    }

    public void setFilename(String filename) {
        if (this.filename != null) {
            throw new FimsRuntimeException("Server Error", "You can only upload 1 fims metadata dataset at a time", 500);
        }
        this.filename = filename;
    }

    public String getName() {
        return NAME;
    }

    /**
     * prepares the fimsMetadata array for indexing via elasticsearch.
     * This adds the bcid and expeditionCode to each resource
     *
     * @return
     */
    public JSONArray index() {
        JSONArray dataset = getDataset();

        Entity rootEntity = processController.getMapping().getRootEntity();
        Bcid rootEntityBcid = expeditionService.getEntityBcid(
                processController.getExpeditionCode(), processController.getProjectId(), rootEntity.getConceptAlias());

        if (rootEntityBcid == null) {
            throw new FimsRuntimeException("Server Error", "rootEntityBcid is null", 500);
        }

        return prepareIndex(dataset, String.valueOf(rootEntityBcid.getIdentifier()));
    }

    public void close() {
        if (filename != null) {
            new File(filename).delete();
        }
    }

    /**
     * prepare all resources in a dataset for indexing. This involves:
     * <p>
     * 1. transforming each column -> uri if possible
     * 2. denormalizing each resource for elasticsearch by:
     * <p>
     * 1. adding expedition.expeditionCode
     * 2. adding bcid
     *
     * @param dataset
     * @param rootIdentifier
     * @return
     */
    private JSONArray prepareIndex(JSONArray dataset, String rootIdentifier) {
        JSONArray index = new JSONArray();

        Map<String, String> columnToUriMap = getColumnToUriMap();
        String uniqueKey = processController.getMapping().getDefaultSheetUniqueKey();

        for (Object o : dataset) {
            JSONObject resource = (JSONObject) o;
            JSONObject resourceIndex = new JSONObject();

            for (Map.Entry entry : (Set<Map.Entry>) resource.entrySet()) {
                Object key = entry.getKey();

                if (columnToUriMap.containsKey(entry.getKey())) {
                    key = columnToUriMap.get(entry.getKey());
                }

                resourceIndex.put(key, entry.getValue());

            }

            resourceIndex.put("expedition.expeditionCode", processController.getExpeditionCode());
            resourceIndex.put("bcid", String.valueOf(rootIdentifier) + resource.get(uniqueKey));

            index.add(resourceIndex);
        }

        return index;
    }

    /**
     * generate a map of ("column", "uri") pairs for each attribute in the {@link Mapping#getDefaultSheetAttributes()}
     * for easy lookup of attribute column to uri
     *
     * @return
     */
    private Map<String, String> getColumnToUriMap() {
        Map<String, String> columnToUriMap = new HashMap<>();
        List<Attribute> attributes = processController.getMapping().getDefaultSheetAttributes();

        for (Attribute attribute : attributes) {
            columnToUriMap.put(attribute.getColumn(), attribute.getUri());
        }

        return columnToUriMap;
    }
}
