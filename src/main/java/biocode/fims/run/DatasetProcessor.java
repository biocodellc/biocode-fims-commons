package biocode.fims.run;

import biocode.fims.fimsExceptions.errorCodes.FileCode;
import biocode.fims.fimsExceptions.errorCodes.ValidationCode;
import biocode.fims.models.Expedition;
import biocode.fims.fimsExceptions.FimsRuntimeException;
import biocode.fims.fimsExceptions.errorCodes.UploadCode;
import biocode.fims.models.User;
import biocode.fims.models.records.*;
import biocode.fims.projectConfig.ProjectConfig;
import biocode.fims.reader.DataReaderFactory;
import biocode.fims.validation.messages.EntityMessages;
import biocode.fims.repositories.RecordRepository;
import biocode.fims.service.ExpeditionService;
import biocode.fims.settings.PathManager;
import biocode.fims.utils.FileUtils;
import biocode.fims.validation.DatasetValidator;
import biocode.fims.validation.RecordValidatorFactory;


import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.*;

/**
 * Validate and Upload Datasets
 */
public class DatasetProcessor {
    private final DataReaderFactory readerFactory;
    private final RecordValidatorFactory validatorFactory;
    private final RecordRepository recordRepository;
    private final ExpeditionService expeditionService;
    private final ProjectConfig projectConfig;
    private final ProcessorStatus processorStatus;

    private final String expeditionCode;
    private final int projectId;
    private final User user;
    private final String workbookFile;
    private final Map<String, RecordMetadata> datasetSources;
    private final boolean reloadWorkbooks;
    private final boolean ignoreUser;
    private final String serverDataDir;
    private Dataset dataset;
    private boolean hasError = false;
    private List<EntityMessages> messages;

    private DatasetProcessor(Builder builder) {
        expeditionCode = builder.expeditionCode;
        projectId = builder.projectId;
        user = builder.user;
        readerFactory = builder.readerFactory;
        validatorFactory = builder.validatorFactory;
        recordRepository = builder.recordRepository;
        expeditionService = builder.expeditionService;
        projectConfig = builder.projectConfig;
        processorStatus = builder.processorStatus;
        workbookFile = builder.workbookFile;
        datasetSources = builder.datasets;
        reloadWorkbooks = builder.reloadWorkbooks;
        ignoreUser = builder.ignoreUser;
        serverDataDir = builder.serverDataDir;
        messages = Collections.emptyList();
    }

    public boolean validate() {
        processorStatus.appendStatus("\nValidating...\n");

        DatasetBuilder datasetBuilder = new DatasetBuilder(readerFactory, recordRepository, projectConfig,
                projectId, expeditionCode)
                .reloadWorkbooks(reloadWorkbooks)
                .addWorkbook(workbookFile);

        for (Map.Entry<String, RecordMetadata> dataset : datasetSources.entrySet()) {
            datasetBuilder.addDatasource(dataset.getKey(), dataset.getValue());
        }

        dataset = datasetBuilder.build();

        DatasetValidator validator = new DatasetValidator(validatorFactory, dataset, projectConfig);

        boolean valid = validator.validate(processorStatus);

        if (!valid) {
            hasError = validator.hasError();
            messages = validator.messages();
        }

        return valid;
    }

    public boolean upload() {
        if (dataset == null) {
            if (!validate() && hasError) {
                return false;
            }
        }

        if (hasError) {
            throw new FimsRuntimeException(ValidationCode.INVALID_DATASET, 500);
        }

        if (user == null) {
            throw new FimsRuntimeException("you must be logged in to upload", 400);
        }

        Expedition expedition = expeditionService.getExpedition(expeditionCode, projectId);

        if (expedition == null) {
            throw new FimsRuntimeException(UploadCode.INVALID_EXPEDITION, 400, expeditionCode);
        } else if (!ignoreUser) {
            if (expedition.getUser().getUserId() != user.getUserId()) {
                throw new FimsRuntimeException(UploadCode.USER_NO_OWN_EXPEDITION, 400, expeditionCode);
            }
        }

        recordRepository.saveDataset(dataset, projectId, expedition.getExpeditionId());

        writeDataSources();
        return true;
    }

    public boolean hasError() {
        return hasError;
    }

    public List<EntityMessages> messages() {
        return messages;
    }

    public String expeditionCode() {
        return expeditionCode;
    }

    public ProcessorStatus status() {
        return processorStatus;
    }

    private void writeDataSources() {

        if (workbookFile != null) {
            writeFileToServer(workbookFile);
        }

        for (Map.Entry<String, RecordMetadata> entry : datasetSources.entrySet()) {
            writeFileToServer(entry.getKey());
        }
    }

    private void writeFileToServer(String file) {
        String ext = FileUtils.getExtension(file, null);
        String filename = "project_" + projectId + "_expedition_" + expeditionCode + "_dataSource." + ext;
        File outputFile = PathManager.createUniqueFile(filename, serverDataDir);

        try {
            Files.copy(new File(file).toPath(), outputFile.toPath());
        } catch (IOException e) {
            throw new FimsRuntimeException(FileCode.WRITE_ERROR, 500);
        }
    }

    public static class Builder {
        // Required
        private int projectId;
        private String expeditionCode;
        private DataReaderFactory readerFactory;
        private RecordValidatorFactory validatorFactory;
        private RecordRepository recordRepository;
        private ProjectConfig projectConfig;
        private ProcessorStatus processorStatus;
        private ExpeditionService expeditionService;
        private String serverDataDir;

        private String workbookFile;
        private Map<String, RecordMetadata> datasets;

        // Optional
        private User user;
        private boolean reloadWorkbooks = false;
        private boolean ignoreUser = false;

        public Builder(int projectId, String expeditionCode, ProcessorStatus processorStatus) {
            this.projectId = projectId;
            this.expeditionCode = expeditionCode;
            this.processorStatus = processorStatus;
            this.datasets = new HashMap<>();
        }

        public Builder readerFactory(DataReaderFactory readerFactory) {
            this.readerFactory = readerFactory;
            return this;
        }

        public Builder validatorFactory(RecordValidatorFactory validatorFactory) {
            this.validatorFactory = validatorFactory;
            return this;
        }

        public Builder recordRepository(RecordRepository recordRepository) {
            this.recordRepository = recordRepository;
            return this;
        }

        public Builder expeditionService(ExpeditionService expeditionService) {
            this.expeditionService = expeditionService;
            return this;
        }

        public Builder projectConfig(ProjectConfig projectConfig) {
            this.projectConfig = projectConfig;
            return this;
        }

        public Builder workbook(String workbookFile) {
            this.workbookFile = workbookFile;
            return this;
        }

        public Builder addDataset(String datasetFile, RecordMetadata metadata) {
            datasets.put(datasetFile, metadata);
            return this;
        }

        public Builder user(User user) {
            this.user = user;
            return this;
        }

        public Builder ignoreUser(boolean ignoreUser) {
            this.ignoreUser = ignoreUser;
            return this;
        }

        /**
         * Is this a complete dataset reload? All previous records will be deleted if the entity allows
         *
         * @param reloadWorkbooks
         * @return
         */
        public Builder reloadWorkbooks(boolean reloadWorkbooks) {
            this.reloadWorkbooks = reloadWorkbooks;
            return this;
        }

        public Builder serverDataDir(String serverRoot) {
            this.serverDataDir = serverRoot;
            return this;
        }

        private boolean isValid() {
            //TODO need to handle cases w/ parent entities & null expeditionCode
//            return expeditionCode != null &&
            return processorStatus != null &&
                    validatorFactory != null &&
                    recordRepository != null &&
                    readerFactory != null &&
                    expeditionService != null &&
                    serverDataDir != null &&
                    projectConfig != null &&
                    (workbookFile != null || datasets.size() > 0);
        }

        public DatasetProcessor build() {
            if (isValid()) {
                return new DatasetProcessor(this);
            } else {
                throw new FimsRuntimeException("Server Error", "validatorFactory, readerFactory, recordRepository, " +
                        "expeditionService, projectConfig must not be null and either a workbook or dataset are required.", 500);
            }
        }
    }
}
