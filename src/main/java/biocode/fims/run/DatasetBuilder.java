package biocode.fims.run;

import biocode.fims.projectConfig.models.Entity;
import biocode.fims.fimsExceptions.FimsRuntimeException;
import biocode.fims.fimsExceptions.errorCodes.FileCode;
import biocode.fims.fimsExceptions.errorCodes.ValidationCode;
import biocode.fims.records.Record;
import biocode.fims.records.RecordMetadata;
import biocode.fims.records.RecordSet;
import biocode.fims.projectConfig.ProjectConfig;
import biocode.fims.reader.*;
import biocode.fims.reader.plugins.ExcelReader;
import biocode.fims.repositories.RecordRepository;
import biocode.fims.utils.FileUtils;
import biocode.fims.validation.rules.UniqueValueRule;

import java.util.*;
import java.util.stream.Collectors;

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
    private final DataConverterFactory dataConverterFactory;
    private final RecordRepository recordRepository;
    private ProjectConfig config;
    private final int projectId;
    private final String expeditionCode;

    private boolean reloadWorkbooks = false;
    private final ArrayList<String> workbooks;
    private final ArrayList<DataSource> dataSources;
    private final ArrayList<RecordSet> recordSets;
    private final ArrayList<String> projectRecordEntities;
    private final ArrayList<String> reloadedEntities;

    public DatasetBuilder(DataReaderFactory dataReaderFactory, DataConverterFactory dataConverterFactory, RecordRepository repository, ProjectConfig config,
                          int projectId, String expeditionCode) {
        this.dataReaderFactory = dataReaderFactory;
        this.dataConverterFactory = dataConverterFactory;
        this.recordRepository = repository;
        this.config = config;
        this.projectId = projectId;
        this.expeditionCode = expeditionCode;

        this.workbooks = new ArrayList<>();
        this.dataSources = new ArrayList<>();
        this.recordSets = new ArrayList<>();
        this.projectRecordEntities = new ArrayList<>();
        this.reloadedEntities = new ArrayList<>();
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

        findEntitiesToFetchProjectRecords();
        instantiateWorkbookRecords();
        instantiateDataSourceRecords();
        addParentRecords();
        setRecordSetParent();

        if (recordSets.isEmpty()) {
            throw new FimsRuntimeException(ValidationCode.EMPTY_DATASET, 400);
        }

        return new Dataset(recordSets);
    }

    private void findEntitiesToFetchProjectRecords() {
        for (Entity e : config.entities()) {
            if (e.getUniqueAcrossProject() || e.getRules().stream()
                    .filter(r -> r instanceof UniqueValueRule)
                    .anyMatch(r -> ((UniqueValueRule) r).uniqueAcrossProject())
                    ) {
                projectRecordEntities.add(e.getConceptAlias());
            }
        }

    }

    private void instantiateDataSourceRecords() {
        for (DataSource dataSource : dataSources) {
            readData(dataSource.dataFile, dataSource.metadata);
        }
    }

    private void instantiateWorkbookRecords() {
        for (String file : workbooks) {
            readData(file, new RecordMetadata(TabularDataReaderType.READER_TYPE, reloadWorkbooks));
        }
    }

    private void readData(String file, RecordMetadata metadata) {
        DataReader reader = dataReaderFactory.getReader(file, config, metadata);

        for (RecordSet set : reader.getRecordSets()) {
            DataConverter converter = dataConverterFactory.getConverter(set.entity().type(), config);
            RecordSet recordSet = converter.convertRecordSet(set, projectId, expeditionCode);
            recordSets.add(recordSet);

            for (Record record : recordSet.records()) {
                record.setExpeditionCode(expeditionCode);
                record.setProjectId(projectId);
            }

            Entity e = recordSet.entity();
            if (metadata.reload()) {
                reloadedEntities.add(e.getConceptAlias());
            }

            if (recordSet.records().size() > 0 && projectRecordEntities.contains(e.getConceptAlias())) {
                List<? extends Record> records = recordRepository.getRecords(projectId, e.getConceptAlias(), e.getRecordType());

                // if we are reloading the dataset, we need to exclude any records for the expedition we're reloading
                if (metadata.reload()) {
                    records = records.stream()
                            .filter(r -> !r.expeditionCode().equals(expeditionCode))
                            .collect(Collectors.toList());
                }

                mergeRecords(e, records);
            }
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

        String parentUniqueKeyURI = entity.isChildEntity() ? config.entity(entity.getParentEntity()).getUniqueKeyURI() : null;
        recordSet.merge(records, parentUniqueKeyURI);

    }

    private void setRecordSetParent() {
        List<RecordSet> recordSetsToRemove = new ArrayList<>();
        List<RecordSet> recordSetsToAdd = new ArrayList<>();

        for (RecordSet r : recordSets) {

            Entity e = r.entity();

            if (e.isChildEntity()) {
                RecordSet parentRecordSet = getRecordSet(e.getParentEntity());
                Entity parentEntity = parentRecordSet.entity();

                // if we've fetched project records for the child and the parent is reloading
                // we need to remove any fetched records that will be deleted b/c the parent is removed
                if (projectRecordEntities.contains(e.getConceptAlias()) && reloadedEntities.contains(e.getParentEntity())) {
                    List<String> parentIdentifiers = parentRecordSet.recordsToPersist().stream()
                            .map(record -> record.get(parentEntity.getUniqueKeyURI()))
                            .collect(Collectors.toList());

                    List<Record> childrenToKeep = r.records().stream()
                            .filter(record -> !record.expeditionCode().equals(expeditionCode)
                                    || parentIdentifiers.contains(record.get(parentEntity.getUniqueKeyURI())))
                            .collect(Collectors.toList());

                    // we can't update a RecordSet records, so we create a new one
                    RecordSet newRecordSet = new RecordSet(e, childrenToKeep, r.reload());
                    newRecordSet.setParent(parentRecordSet);
                    recordSetsToAdd.add(newRecordSet);
                    recordSetsToRemove.add(r);
                } else {
                    r.setParent(parentRecordSet);
                }
            }
        }

        recordSets.removeAll(recordSetsToRemove);
        recordSets.addAll(recordSetsToAdd);
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
