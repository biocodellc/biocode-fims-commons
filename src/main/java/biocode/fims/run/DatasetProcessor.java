package biocode.fims.run;

import biocode.fims.application.config.FimsAppConfig;
import biocode.fims.fimsExceptions.errorCodes.ValidationCode;
import biocode.fims.models.Expedition;
import biocode.fims.fimsExceptions.FimsRuntimeException;
import biocode.fims.fimsExceptions.errorCodes.UploadCode;
import biocode.fims.models.Project;
import biocode.fims.models.records.*;
import biocode.fims.projectConfig.ProjectConfig;
import biocode.fims.reader.DataReaderFactory;
import biocode.fims.reader.TabularDataReaderType;
import biocode.fims.reader.plugins.CSVReader;
import biocode.fims.renderers.EntityMessages;
import biocode.fims.repositories.RecordRepository;
import biocode.fims.service.ExpeditionService;
import biocode.fims.service.ProjectService;
import biocode.fims.validation.DatasetValidator;
import biocode.fims.validation.RecordValidatorFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;


import java.util.*;

/**
 * Class for processing datasets. This includes validation and uploading
 */
public class DatasetProcessor {
    private static final Logger logger = LoggerFactory.getLogger(DatasetProcessor.class);

    private final DataReaderFactory readerFactory;
    private final RecordValidatorFactory validatorFactory;
    private final RecordRepository recordRepository;
    private final ProjectConfig projectConfig;
    private final ProcessController processController;

    private final String workbookFile;
    private final Map<String, RecordMetadata> datasetSources;
    private final boolean reloadDataset;
    private List<RecordSet> dataset;
    private boolean hasError = false;
    private List<EntityMessages> messages;

    public static class Builder {
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

        public Builder(ProcessController processController) {
            this.processController = processController;
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

        /**
         * Is this a complete dataset reload? All previous records will be deleted
         * @param reloadDataset
         * @return
         */
        public Builder reloadDataset(boolean reloadDataset) {
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

        public DatasetProcessor build() {
            if (isValid()) {
                return new DatasetProcessor(this);
            } else {
                throw new FimsRuntimeException("Server Error", "validatorFactory, readerFactory, recordRepository, " +
                        "projectConfig must not be null and either a workbook or dataset are required.", 500);
            }
        }
    }

    private DatasetProcessor(Builder builder) {
        readerFactory = builder.readerFactory;
        validatorFactory = builder.validatorFactory;
        recordRepository = builder.recordRepository;
        projectConfig = builder.projectConfig;
        processController = builder.processController;
        workbookFile = builder.workbookFile;
        datasetSources = builder.datasets;
        reloadDataset = builder.reloadDataset;
        messages = Collections.emptyList();
    }

    public boolean validate() {
        processController.appendStatus("\nValidating...\n");

        DatasetBuilder datasetBuilder = new DatasetBuilder(readerFactory, recordRepository, projectConfig,
                processController.getProjectId(), processController.getExpeditionCode())
                .reloadDataset(reloadDataset)
                .addWorkbook(workbookFile);

        for (Map.Entry<String, RecordMetadata> dataset: datasetSources.entrySet()) {
            datasetBuilder.addDatasource(dataset.getKey(), dataset.getValue());
        }

        dataset = datasetBuilder.build();

        // TODO add status messages during validation
        DatasetValidator validator = new DatasetValidator(validatorFactory, dataset, projectConfig);

        boolean valid = validator.validate();

        if (!valid) {
            hasError = true;
            messages = validator.messages();
        }

        return valid;
    }

    public void upload(boolean createExpedition, boolean ignoreUser, ExpeditionService expeditionService) {
        if (dataset == null) {
            throw new IllegalStateException("validate must be called before uploading");
        }

        if (hasError) {
            throw new FimsRuntimeException(ValidationCode.INVALID_DATASET, 500);
        }

        if (createExpedition) {
            createExpedition(expeditionService);
        }

        Expedition expedition = getExpedition(expeditionService);

        if (expedition == null) {
            throw new FimsRuntimeException(UploadCode.EXPEDITION_CREATE, 400, processController.getExpeditionCode());
        } else if (!ignoreUser) {
            if (expedition.getUser().getUserId() != processController.getUserId()) {
                throw new FimsRuntimeException(UploadCode.USER_NO_OWN_EXPEDITION, 400, processController.getExpeditionCode());
            }
        }

        recordRepository.save(dataset, expedition.getProject().getProjectCode(), expedition.getExpeditionId());

//        processController.appendSuccessMessage("<br><font color=#188B00>Successfully Uploaded!</font><br><br>");

    }

    public boolean hasError() {
        return hasError;
    }

    public List<EntityMessages> messages() {
        return messages;
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
                .expeditionTitle("Expedition " + processController.getExpeditionCode())
                .isPublic(processController.getPublicStatus())
                .build();

        expeditionService.create(
                expedition,
                processController.getUserId(),
                processController.getProjectId(),
                null
        );
    }

    public static void main(String[] args) {
        AnnotationConfigApplicationContext applicationContext = new AnnotationConfigApplicationContext(FimsAppConfig.class);
        ProjectService projectService = applicationContext.getBean(ProjectService.class);
        DataReaderFactory dataReaderFactory = applicationContext.getBean(DataReaderFactory.class);
        RecordValidatorFactory validatorFactory = applicationContext.getBean(RecordValidatorFactory.class);
        RecordRepository repository = applicationContext.getBean(RecordRepository.class);

        String file = "/Users/rjewing/Desktop/geome-fims-output.txt";
        RecordMetadata metadata = new RecordMetadata(TabularDataReaderType.READER_TYPE);
        metadata.add(CSVReader.SHEET_NAME_KEY, "Samples");
        Project project = projectService.getProject(25);

        DatasetBuilder datasetBuilder = new DatasetBuilder(dataReaderFactory, repository, project.getProjectConfig(), 25, "TEST")
                .addDatasource(file, metadata);

        List<RecordSet> recordSets = datasetBuilder.build();

        DatasetValidator validator = new DatasetValidator(validatorFactory, recordSets, project.getProjectConfig());

        if (validator.validate() || !validator.hasError()) {
            repository.save(recordSets, project.getProjectCode(), 30);
        }

        validator.hasError();
    }
}
