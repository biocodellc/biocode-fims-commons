package biocode.fims.run;

import biocode.fims.fimsExceptions.errorCodes.FileCode;
import biocode.fims.fimsExceptions.errorCodes.ValidationCode;
import biocode.fims.models.Expedition;
import biocode.fims.fimsExceptions.FimsRuntimeException;
import biocode.fims.fimsExceptions.errorCodes.UploadCode;
import biocode.fims.models.Project;
import biocode.fims.models.User;
import biocode.fims.reader.DataConverterFactory;
import biocode.fims.reader.DataReaderFactory;
import biocode.fims.reader.TabularDataReaderType;
import biocode.fims.records.RecordMetadata;
import biocode.fims.records.RecordSet;
import biocode.fims.validation.messages.EntityMessages;
import biocode.fims.repositories.RecordRepository;
import biocode.fims.utils.FileUtils;
import biocode.fims.validation.DatasetValidator;
import biocode.fims.validation.RecordValidatorFactory;
import biocode.fims.validation.messages.Message;
import org.apache.commons.collections.keyvalue.MultiKey;


import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Validate and Upload Datasets
 */
public class DatasetProcessor {
    private final DataReaderFactory readerFactory;
    private final DataConverterFactory dataConverterFactory;
    private final RecordValidatorFactory validatorFactory;
    private final DatasetAuthorizer datasetAuthorizer;
    private final RecordRepository recordRepository;
    private final ProcessorStatus processorStatus;

    private final String expeditionCode;
    private final Project project;
    private final User user;
    private final String workbookFile;
    private final Map<String, RecordMetadata> datasetSources;
    private final List<RecordSet> recordSets;
    private final boolean reloadWorkbooks;
    private final boolean isUpload;
    private final boolean writeToServer;
    private final boolean uploadValid;
    private final String serverDataDir;
    private Dataset dataset;
    private boolean hasError = false;
    private List<EntityMessages> messages;

    private DatasetProcessor(Builder builder) {
        expeditionCode = builder.expeditionCode;
        project = builder.project;
        user = builder.user;
        readerFactory = builder.readerFactory;
        dataConverterFactory = builder.dataConverterFactory;
        validatorFactory = builder.validatorFactory;
        datasetAuthorizer = builder.datasetAuthorizer;
        recordRepository = builder.recordRepository;
        processorStatus = builder.processorStatus;
        workbookFile = builder.workbookFile;
        datasetSources = builder.datasets;
        recordSets = builder.recordSets;
        reloadWorkbooks = builder.reloadWorkbooks;
        isUpload = builder.isUpload;
        writeToServer = builder.writeToServer;
        uploadValid = builder.uploadValid;
        serverDataDir = builder.serverDataDir;
        messages = new ArrayList<>();
    }

    public boolean validate() {
        processorStatus.appendStatus("\nValidating...\n");

        DatasetBuilder datasetBuilder = new DatasetBuilder(readerFactory, dataConverterFactory,
                recordRepository, project, expeditionCode)
                .reloadWorkbooks(reloadWorkbooks)
                .addWorkbook(workbookFile);

        for (Map.Entry<String, RecordMetadata> dataset : datasetSources.entrySet()) {
            if (isUpload && expeditionCode == null && !TabularDataReaderType.READER_TYPE.equals(dataset.getValue().readerType())) {
                throw new FimsRuntimeException(ValidationCode.INVALID_DATASET, 400, "expeditionCode is required if you are uploading a non-tabular Dataset");
            }
            datasetBuilder.addDatasource(dataset.getKey(), dataset.getValue());
        }

        for (RecordSet recordSet : recordSets) {
            datasetBuilder.addRecordSet(recordSet);
        }

        dataset = datasetBuilder.build();

        DatasetValidator validator = new DatasetValidator(validatorFactory, dataset, project.getProjectConfig());

        boolean valid = validator.validate(processorStatus);

        if (!valid) {
            hasError = validator.hasError();
            messages = validator.messages();
        }

        if (datasetBuilder.mismatchedExpeditions().size() > 0) {
            valid = false;
            for (Map.Entry<MultiKey, Set<String>> e : datasetBuilder.mismatchedExpeditions().entrySet()) {
                String conceptAlias = (String) e.getKey().getKey(0);
                String worksheet = (String) e.getKey().getKey(1);

                List<EntityMessages> entityMessages = messages.stream()
                        .filter(m -> Objects.equals(m.conceptAlias(), conceptAlias) && Objects.equals(m.sheetName(), worksheet))
                        .collect(Collectors.toList());

                if (entityMessages.isEmpty()) {
                    EntityMessages m = new EntityMessages(conceptAlias, worksheet);
                    m.addWarningMessage(
                            "Mismatched expeditionCode(s)",
                            new Message("We found the following expeditionCodes in this worksheet that differ from the specified upload expedition: " + e.getValue())
                    );
                    messages.add(m);
                } else {
                    entityMessages.forEach(m ->
                            m.addWarningMessage(
                                    "Mismatched expeditionCode(s)",
                                    new Message("We found the following expeditionCodes in this worksheet that differ from the specified upload expedition: " + e.getValue())
                            )
                    );
                }
            }
        }

        return valid;
    }

    public boolean upload() {
        if (dataset == null) {
            if (!validate() && hasError && !uploadValid) {
                return false;
            }
        }

        if (hasError && !uploadValid) {
            throw new FimsRuntimeException(ValidationCode.INVALID_DATASET, 500, "Server Error");
        }

        if (!datasetAuthorizer.authorize(dataset, project, user)) {
            throw new FimsRuntimeException(UploadCode.UNAUTHORIZED, 400);
        }

        recordRepository.saveDataset(dataset, project.getNetwork().getId());

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

    public Dataset dataset() {
        return dataset;
    }

    private void writeDataSources() {
        if (!writeToServer) return;

        if (workbookFile != null) {
            writeFileToServer(workbookFile);
        }

        for (Map.Entry<String, RecordMetadata> entry : datasetSources.entrySet()) {
            writeFileToServer(entry.getKey());
        }
    }

    private void writeFileToServer(String file) {
        String ext = FileUtils.getExtension(file, null);
        String filename = "project_" + project.getProjectId() + "_expedition_" + expeditionCode + "_dataSource." + ext;
        File outputFile = FileUtils.createUniqueFile(filename, serverDataDir);

        try {
            Files.copy(Paths.get(file), outputFile.toPath());
        } catch (IOException e) {
            throw new FimsRuntimeException(FileCode.WRITE_ERROR, 500, e);
        }
    }

    public static class Builder {
        // Required
        private Project project;
        private String expeditionCode;
        private DataReaderFactory readerFactory;
        private DataConverterFactory dataConverterFactory;
        private RecordValidatorFactory validatorFactory;
        private DatasetAuthorizer datasetAuthorizer;
        private RecordRepository recordRepository;
        private ProcessorStatus processorStatus;
        private String serverDataDir;

        private String workbookFile;
        private Map<String, RecordMetadata> datasets;
        private List<RecordSet> recordSets;

        // Optional
        private User user;
        private boolean reloadWorkbooks = false;
        private boolean isUpload = false;
        private boolean writeToServer = false;
        private boolean uploadValid = false;

        public Builder(Project project, String expeditionCode, ProcessorStatus processorStatus) {
            this.project = project;
            this.expeditionCode = expeditionCode;
            this.processorStatus = processorStatus;
            this.datasets = new HashMap<>();
            this.recordSets = new ArrayList<>();
        }

        public Builder readerFactory(DataReaderFactory readerFactory) {
            this.readerFactory = readerFactory;
            return this;
        }

        public Builder dataConverterFactory(DataConverterFactory dataConverterFactory) {
            this.dataConverterFactory = dataConverterFactory;
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

        public Builder datasetAuthorizer(DatasetAuthorizer authorizer) {
            this.datasetAuthorizer = authorizer;
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

        public Builder addRecordSet(RecordSet r) {
            recordSets.add(r);
            return this;
        }

        public Builder user(User user) {
            this.user = user;
            return this;
        }

        public Builder upload() {
            this.isUpload = true;
            return this;
        }

        /**
         * Upload any records that don't have a validation error
         *
         * @return
         */
        public Builder uploadValid() {
            upload();
            this.uploadValid = true;
            return this;
        }

        /**
         * Save Dataset & Workbook files to the server
         *
         * @return
         */
        public Builder writeToServer() {
            this.writeToServer = true;
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
                    datasetAuthorizer != null &&
                    dataConverterFactory != null &&
                    recordRepository != null &&
                    (recordSets.size() > 0 || readerFactory != null) &&
                    serverDataDir != null &&
                    project != null &&
                    (workbookFile != null || datasets.size() > 0 || recordSets.size() > 0);
        }

        public DatasetProcessor build() {
            if (isValid()) {
                return new DatasetProcessor(this);
            } else {
                throw new FimsRuntimeException("Server Error", "validatorFactory, readerFactory, recordRepository, " +
                        "datasetAuthorizer, dataConverterFactory, project must not be null and either a workbook, dataset, or recordSet are required.", 500);
            }
        }
    }
}
