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
    private final boolean reloadDataset;
    private final boolean ignoreUser;
    private final boolean publicStatus;
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
        reloadDataset = builder.reloadDataset;
        ignoreUser = builder.ignoreUser;
        publicStatus = builder.publicStatus;
        serverDataDir = builder.serverDataDir;
        messages = Collections.emptyList();
    }

    public boolean validate() {
        processorStatus.appendStatus("\nValidating...\n");

        DatasetBuilder datasetBuilder = new DatasetBuilder(readerFactory, recordRepository, projectConfig,
                projectId, expeditionCode)
                .reloadDataset(reloadDataset)
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

    public boolean upload(boolean createExpedition) {
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

        if (createExpedition) {
            createExpedition(expeditionService);
        }

        Expedition expedition = expeditionService.getExpedition(expeditionCode, projectId);

        if (expedition == null) {
            throw new FimsRuntimeException(UploadCode.EXPEDITION_CREATE, 400, expeditionCode);
        } else if (!ignoreUser) {
            if (expedition.getUser().getUserId() != user.getUserId()) {
                throw new FimsRuntimeException(UploadCode.USER_NO_OWN_EXPEDITION, 400, expeditionCode);
            }
        }

        recordRepository.save(dataset, expedition.getProject().getProjectId(), expedition.getExpeditionId());

        if (expedition.isPublic() != publicStatus) {
            expedition.setPublic(publicStatus);
            expeditionService.update(expedition);
        }

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

    private void createExpedition(ExpeditionService expeditionService) {
        Expedition expedition = expeditionService.getExpedition(expeditionCode, projectId);

        if (expedition != null) {
            if (expedition.getUser() != user) {
                throw new FimsRuntimeException(UploadCode.USER_NO_OWN_EXPEDITION, 400, expeditionCode);
            } else {
                // expedition already exists and the user owns the expedition
                return;
            }
        }

        String status = "\n\tCreating expedition " + expeditionCode + " ... this is a one time process " +
                "before loading each spreadsheet and may take a minute...\n";
        processorStatus.appendStatus(status);

        expedition = new Expedition.ExpeditionBuilder(
                expeditionCode)
                .expeditionTitle(expeditionCode + " Dataset")
                .isPublic(publicStatus)
                .build();

        expeditionService.create(
                expedition,
                user.getUserId(),
                projectId,
                null
        );
    }

    private void writeDataSources() {

        if (workbookFile != null) {
            writeFileToServer(workbookFile);
        }

        for (Map.Entry<String, RecordMetadata> entry :datasetSources.entrySet()) {
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
        private boolean reloadDataset = false;
        private boolean ignoreUser = false;
        private boolean publicStatus = false;

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

        public Builder publicStatus(boolean publicStatus) {
            this.publicStatus = publicStatus;
            return this;
        }

        /**
         * Is this a complete dataset reload? All previous records will be deleted
         *
         * @param reloadDataset
         * @return
         */
        public Builder reloadDataset(boolean reloadDataset) {
            this.reloadDataset = reloadDataset;
            return this;
        }

        public Builder serverDataDir(String serverRoot) {
            this.serverDataDir = serverRoot;
            return this;
        }

        private boolean isValid() {
            return expeditionCode != null &&
                    processorStatus != null &&
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
