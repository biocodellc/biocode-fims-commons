package biocode.fims.run;

import biocode.fims.models.Expedition;
import biocode.fims.fimsExceptions.FimsRuntimeException;
import biocode.fims.fimsExceptions.errorCodes.UploadCode;
import biocode.fims.models.records.*;
import biocode.fims.projectConfig.ProjectConfig;
import biocode.fims.reader.DataReaderFactory;
import biocode.fims.repositories.RecordRepository;
import biocode.fims.service.ExpeditionService;
import biocode.fims.validation.DatasetValidator;
import biocode.fims.validation.RecordValidatorFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import java.util.*;

/**
 * Core class for running fims validation and uploading.
 */
public class Process {
    private static final Logger logger = LoggerFactory.getLogger(Process.class);

    private final DataReaderFactory readerFactory;
    private final RecordValidatorFactory validatorFactory;
    private final RecordRepository recordRepository;
    private final ProjectConfig projectConfig;
    private final ProcessController processController;

    private final String workbookFile;
    private final Map<String, RecordMetadata> datasets;
    private final boolean reloadDataset;

    public static class ProcessBuilder {
        // Required
        private DataReaderFactory readerFactory;
        private RecordValidatorFactory validatorFactory;
        private RecordRepository recordRepository;
        private ProjectConfig projectConfig;
        private ProcessController processController;

        private String workbookFile;
        private Map<String, RecordMetadata> datasets;

        // Optional
        private boolean reloadDataset = false;

        public ProcessBuilder(ProcessController processController) {
            this.processController = processController;
            this.datasets = new HashMap<>();
        }

        public ProcessBuilder readerFactory(DataReaderFactory readerFactory) {
            this.readerFactory = readerFactory;
            return this;
        }

        public ProcessBuilder validatorFactory(RecordValidatorFactory validatorFactory) {
            this.validatorFactory = validatorFactory;
            return this;
        }

        public ProcessBuilder recordRepository(RecordRepository recordRepository) {
            this.recordRepository = recordRepository;
            return this;
        }

        public ProcessBuilder projectConfig(ProjectConfig projectConfig) {
            this.projectConfig = projectConfig;
            return this;
        }

        public ProcessBuilder workbook(String workbookFile) {
            this.workbookFile = workbookFile;
            return this;
        }

        public ProcessBuilder addDataset(String datasetFile, RecordMetadata metadata) {
            datasets.put(datasetFile, metadata);
            return this;
        }

        /**
         * Is this a complete dataset reload? All previous records will be deleted
         * @param reloadDataset
         * @return
         */
        public ProcessBuilder reloadDataset(boolean reloadDataset) {
            this.reloadDataset = reloadDataset;
            return this;
        }

        private boolean isValid() {
            return validatorFactory != null &&
                    recordRepository != null &&
                    readerFactory != null &&
                    projectConfig != null &&
                    (workbookFile != null || datasets.size() > 0);
        }

        public Process build() {
            if (isValid()) {
                return new Process(this);
            } else {
                throw new FimsRuntimeException("Server Error", "validatorFactory, readerFactory, recordRepository, " +
                        "projectConfig must not be null and either a workbook or dataset are required.", 500);
            }
        }
    }

    private Process(ProcessBuilder builder) {
        readerFactory = builder.readerFactory;
        validatorFactory = builder.validatorFactory;
        recordRepository = builder.recordRepository;
        projectConfig = builder.projectConfig;
        processController = builder.processController;
        workbookFile = builder.workbookFile;
        datasets = builder.datasets;
        reloadDataset = builder.reloadDataset;

        //TODO maybe need to get/set projectConfig on projectController
    }

    public boolean validate() {
        processController.appendStatus("\nValidating...\n");

        DatasetBuilder datasetBuilder = new DatasetBuilder(readerFactory, recordRepository, projectConfig,
                processController.getProjectId(), processController.getExpeditionCode())
                .reloadDataset(reloadDataset)
                .addWorkbook(workbookFile);

        for (Map.Entry<String, RecordMetadata> dataset: datasets.entrySet()) {
            datasetBuilder.addDatasource(dataset.getKey(), dataset.getValue());
        }

        List<RecordSet> dataset = datasetBuilder.build();

        // TODO add status messages during validation
        DatasetValidator validator = new DatasetValidator(validatorFactory, dataset, projectConfig);

        boolean valid = validator.validate();

        if (!valid) {
            processController.setHasError(validator.hasError());
            processController.setMessages(validator.messages());
        }

        return valid;
    }

    public void upload(boolean createExpedition, boolean ignoreUser, ExpeditionService expeditionService) {
        //TODO implement this
//        if (createExpedition) {
//            createExpedition(expeditionService);
//        }
//
//        Expedition expedition = getExpedition(expeditionService);
//
//        if (expedition == null) {
//            throw new FimsRuntimeException(UploadCode.EXPEDITION_CREATE, 400, processController.getExpeditionCode());
//        } else if (!ignoreUser) {
//            if (expedition.getUser().getUserId() != processController.getUserId()) {
//                throw new FimsRuntimeException(UploadCode.USER_NO_OWN_EXPEDITION, 400, processController.getExpeditionCode());
//            }
//        }
//
////        fimsMetadataFileManager.upload();
//
//        processController.appendSuccessMessage("<br><font color=#188B00>Successfully Uploaded!</font><br><br>");

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

        String status = "\n\tCreating expedition " + processController.getExpeditionCode() + " ... this is a one time process " +
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
}
