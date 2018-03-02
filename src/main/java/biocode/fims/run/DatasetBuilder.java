package biocode.fims.run;

import biocode.fims.digester.Entity;
import biocode.fims.fimsExceptions.FimsRuntimeException;
import biocode.fims.fimsExceptions.errorCodes.FileCode;
import biocode.fims.fimsExceptions.errorCodes.ValidationCode;
import biocode.fims.models.records.Record;
import biocode.fims.models.records.RecordMetadata;
import biocode.fims.models.records.RecordSet;
import biocode.fims.projectConfig.ProjectConfig;
import biocode.fims.reader.DataReaderFactory;
import biocode.fims.reader.DataReader;
import biocode.fims.reader.plugins.ExcelReader;
import biocode.fims.reader.TabularDataReaderType;
import biocode.fims.repositories.RecordRepository;
import biocode.fims.utils.FileUtils;

import java.util.*;

/**
 * This class is responsible for taking the input data files and assembling datasets.
 * If there are child entities, we will merge the stored parent records with any records included during the upload.
 * The returned list of RecordSets includes everything needed for validation. All Child and Parent RecordSets
 * will be present
 *
 * @author rjewing
 */
public class DatasetBuilder {

    private final DataReaderFactory dataReaderFactory;
    private final RecordRepository recordRepository;
    private ProjectConfig config;
    private final int projectId;
    private final String expeditionCode;

    private boolean reloadWorkbooks = false;
    private final ArrayList<String> workbooks;
    private final ArrayList<DataSource> dataSources;
    private final ArrayList<RecordSet> recordSets;

    public DatasetBuilder(DataReaderFactory dataReaderFactory, RecordRepository repository, ProjectConfig config,
                          int projectId, String expeditionCode) {
        this.dataReaderFactory = dataReaderFactory;
        this.recordRepository = repository;
        this.config = config;
        this.projectId = projectId;
        this.expeditionCode = expeditionCode;

        this.workbooks = new ArrayList<>();
        this.dataSources = new ArrayList<>();
        this.recordSets = new ArrayList<>();
    }

    public DatasetBuilder addWorkbook(String workbookFile) {
        if (workbookFile == null) return this;

        String ext = FileUtils.getExtension(workbookFile, "");

        if (!ExcelReader.EXTS.contains(ext.toLowerCase())) {
            throw new FimsRuntimeException(FileCode.INVALID_FILE, "File ext is not a valid excel workbook file extension", 400, ext);
        }

        this.workbooks.add(workbookFile);

        return this;
    }

    public DatasetBuilder addDatasource(String dataFile, RecordMetadata recordMetadata) {
        this.dataSources.add(new DataSource(dataFile, recordMetadata));
        return this;
    }

    public DatasetBuilder reloadWorkbooks(boolean reload) {
        this.reloadWorkbooks = reload;
        return this;
    }

    public Dataset build() {

        instantiateWorkbookRecords();
        instantiateDataSourceRecords();
        addParentRecords();
        setRecordSetParent();

        if (recordSets.isEmpty()) {
            throw new FimsRuntimeException(ValidationCode.EMPTY_DATASET, 400);
        }

        return new Dataset(recordSets);
    }

    private void instantiateDataSourceRecords() {
        for (DataSource dataSource : dataSources) {
            DataReader reader = dataReaderFactory.getReader(dataSource.dataFile, config, dataSource.metadata);

            recordSets.addAll(reader.getRecordSets());
        }
    }

    private void instantiateWorkbookRecords() {
        for (String file : workbooks) {
            DataReader reader = dataReaderFactory.getReader(file, config, new RecordMetadata(TabularDataReaderType.READER_TYPE, reloadWorkbooks));

            recordSets.addAll(reader.getRecordSets());
        }
    }

    private void addParentRecords() {
        Set<Entity> parentEntities = getChildRecordSetParentEntities();

        for (Entity e : parentEntities) {

            if (fetchRecordSet(e)) {
                List<? extends Record> records = recordRepository.getRecords(projectId, expeditionCode, e.getConceptAlias(), e.getRecordType());

                mergeRecords(e, records);
            }
        }
    }

    private Set<Entity> getChildRecordSetParentEntities() {
        Set<Entity> parentEntities = new HashSet<>();

        for (RecordSet r : recordSets) {

            Entity e = r.entity();

            if (e.isChildEntity()) {
                parentEntities.add(config.entity(e.getParentEntity()));
            }
        }

        return parentEntities;
    }

    private boolean fetchRecordSet(Entity entity) {
        return recordSets.stream()
                .filter(r -> r.entity().equals(entity))
                .findFirst()
                .map(r -> !r.reload())
                .orElse(true);
    }

    private void mergeRecords(Entity entity, List<? extends Record> records) {
        RecordSet recordSet = recordSets.stream()
                .filter(rs -> rs.entity().equals(entity))
                .findFirst()
                .orElse(null);

        if (recordSet == null) {
            recordSet = new RecordSet(entity, false);
            recordSets.add(recordSet);
        }

        recordSet.merge(records);

    }

    private void setRecordSetParent() {
        for (RecordSet r : recordSets) {

            Entity e = r.entity();

            if (e.isChildEntity()) {
                r.setParent(getRecordSet(e.getParentEntity()));
            }
        }
    }

    private RecordSet getRecordSet(String conceptAlias) {
        for (RecordSet r : recordSets) {
            if (r.conceptAlias().equals(conceptAlias)) {
                return r;
            }
        }

        return new RecordSet(config.entity(conceptAlias), false);
    }

    private static class DataSource {
        private String dataFile;
        private RecordMetadata metadata;

        private DataSource(String dataFile, RecordMetadata metadata) {
            this.dataFile = dataFile;
            this.metadata = metadata;
        }
    }
}
