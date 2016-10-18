package biocode.fims.run;

import biocode.fims.config.ConfigurationFileTester;
import biocode.fims.digester.Mapping;
import biocode.fims.digester.Validation;
import biocode.fims.entities.Expedition;
import biocode.fims.fileManagers.AuxilaryFileManager;
import biocode.fims.fileManagers.FileManager;
import biocode.fims.fileManagers.dataset.IDatasetFileManager;
import biocode.fims.fimsExceptions.FimsRuntimeException;
import biocode.fims.fimsExceptions.UploadCode;
import biocode.fims.service.ExpeditionService;
import org.apache.commons.lang.StringUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Core class for running fims validation and uploading.
 */
public class Process {

    private final List<AuxilaryFileManager> fileManagers;
    private final IDatasetFileManager datasetFileManager;
    private final ProcessController processController;
    private final File configFile;

    public static class ProcessBuilder {
        // Required
        IDatasetFileManager datasetFileManager;
        ProcessController processController;
        Map<String, String> fileMap;
        File configFile;

        // Optional
        List<AuxilaryFileManager> additionalFileManagers = new ArrayList<>();

        public ProcessBuilder(IDatasetFileManager datasetFileManager, ProcessController processController) {
            this.datasetFileManager = datasetFileManager;
            this.processController = processController;
        }

        /**
         * Required.
         *
         * @param configFile
         * @return
         */
        public ProcessBuilder configFile(File configFile) {
            this.configFile = configFile;
            return this;
        }

        /**
         * Required.
         *
         * @param fileMap fileManagerName, filename pairs
         * @return
         */
        public ProcessBuilder addFiles(Map<String, String> fileMap) {
            this.fileMap = fileMap;
            return this;
        }

        public ProcessBuilder addFileManager(AuxilaryFileManager fm) {
            additionalFileManagers.add(fm);
            return this;
        }

        public ProcessBuilder addFileManagers(List<AuxilaryFileManager> fileManagers) {
            additionalFileManagers.addAll(fileManagers);
            return this;
        }


        private boolean isValid() {
            return fileMap != null && configFile != null;
        }

        public Process build() {
            if (isValid()) {
                return new Process(this);
            } else {
                throw new FimsRuntimeException("Server Error", "fileMap, configFile, and outputFolder must not be null.", 500);
            }
        }
    }

    private Process(ProcessBuilder builder) {
        datasetFileManager = builder.datasetFileManager;
        processController = builder.processController;
        fileManagers = builder.additionalFileManagers;
        configFile = builder.configFile;
        addFiles(builder.fileMap);
        addProcessControllerToFileManagers();

        if (processController.getMapping() == null) {
            // Parse the Mapping object (this object is used extensively in downstream functions!)
            Mapping mapping = new Mapping();
            mapping.addMappingRules(configFile);
            processController.setMapping(mapping);
        }

        if (processController.getValidation() == null) {
            // Load validation object as this is used in downstream functions
            Validation validation = new Validation();
            validation.addValidationRules(configFile, processController.getMapping());

            processController.setValidation(validation);
        }
    }

    public boolean validate() {
        processController.appendStatus("\nValidating...\n");

        boolean valid = validateConfigFile() && datasetFileManager.validate();

        for (AuxilaryFileManager fm : fileManagers) {
            if (!fm.validate(datasetFileManager.getDataset())) {
                valid = false;
            }
        }

        return valid;
    }

    public void upload(boolean createExpedition, boolean ignoreUser, ExpeditionService expeditionService) {
        if (createExpedition && datasetFileManager.isNewDataset()) {
            createExpedition(expeditionService);
        }

        Expedition expedition = getExpedition(expeditionService);

        if (expedition == null) {
            if (datasetFileManager.isNewDataset()) {
                throw new FimsRuntimeException(UploadCode.EXPEDITION_CREATE, 400, processController.getExpeditionCode());
            } else {
                throw new FimsRuntimeException(UploadCode.INVALID_EXPEDITION, 400, processController.getExpeditionCode());
            }
        } else if (!ignoreUser) {
            if (expedition.getUser().getUserId() != processController.getUserId()) {
                throw new FimsRuntimeException(UploadCode.USER_NO_OWN_EXPEDITION, 400, processController.getExpeditionCode());
            }
        }

        datasetFileManager.upload();

        for (AuxilaryFileManager fm : fileManagers) {
            fm.upload(datasetFileManager.isNewDataset());
        }

        processController.appendSuccessMessage("<br><font color=#188B00>Successfully Uploaded!</font><br><br>");

    }

    private Expedition getExpedition(ExpeditionService expeditionService) {
        return expeditionService.getExpedition(
                processController.getExpeditionCode(),
                processController.getProjectId()
        );
    }

    private void createExpedition(ExpeditionService expeditionService) {
        Expedition expedition = getExpedition(expeditionService);

        if (expedition != null) {
            throw new FimsRuntimeException(UploadCode.USER_NO_OWN_EXPEDITION, 400, processController.getExpeditionCode());
        }

        String status = "\tCreating expedition " + processController.getExpeditionCode() + " ... this is a one time process " +
                "before loading each spreadsheet and may take a minute...\n";
        processController.appendStatus(status);

        expedition = new Expedition.ExpeditionBuilder(
                processController.getExpeditionCode())
                .expeditionTitle(processController.getExpeditionTitle())
                .isPublic(processController.getPublicStatus())
                .build();

        expeditionService.create(
                expedition,
                processController.getUserId(),
                processController.getProjectId(),
                null,
                processController.getMapping()
        );
    }

    /**
     * test that the config file is valid
     * @return
     */
    private boolean validateConfigFile() {
        ConfigurationFileTester cFT = new ConfigurationFileTester(configFile);

        boolean validConfig = cFT.isValidConfig();
        processController.setConfigMessages(cFT.getMessages());

        return validConfig;
    }

    private void addFiles(Map<String, String> fileMap) {
        for (Map.Entry<String, String> entry : fileMap.entrySet()) {
            FileManager fm = getFileManagerByName(entry.getKey());
            fm.setFilename(entry.getValue());
        }
    }

    private FileManager getFileManagerByName(String fileManagerName) {
        if (StringUtils.equals(datasetFileManager.getName(), fileManagerName)) {
            return datasetFileManager;
        }

        for (FileManager fm : fileManagers) {
            if (StringUtils.equals(fm.getName(), fileManagerName)) {
                return fm;
            }
        }
        throw new FimsRuntimeException("", "No fileManager found with name: " + fileManagerName, 500);
    }

    private void addProcessControllerToFileManagers() {
        datasetFileManager.setProcessController(processController);

        for (FileManager fm : fileManagers) {
            fm.setProcessController(processController);
        }
    }

    public void close() {
        datasetFileManager.close();
        for (FileManager fm: fileManagers) {
            fm.close();;
        }
    }
}
