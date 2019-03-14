package biocode.fims.run;

import biocode.fims.config.EntitySort;
import biocode.fims.config.models.Entity;
import biocode.fims.config.project.ProjectConfig;
import biocode.fims.fimsExceptions.FimsRuntimeException;
import biocode.fims.fimsExceptions.errorCodes.FileCode;
import biocode.fims.fimsExceptions.errorCodes.ValidationCode;
import biocode.fims.models.Project;
import biocode.fims.records.GenericRecord;
import biocode.fims.records.Record;
import biocode.fims.records.RecordMetadata;
import biocode.fims.records.RecordSet;
import biocode.fims.reader.*;
import biocode.fims.reader.plugins.ExcelReader;
import biocode.fims.repositories.RecordRepository;
import biocode.fims.utils.FileUtils;
import biocode.fims.validation.messages.EntityMessages;
import biocode.fims.validation.rules.UniqueValueRule;
import org.apache.commons.collections.keyvalue.MultiKey;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
    private final Project project;
    private final String expeditionCode;

    private boolean reloadWorkbooks = false;
    private final List<String> workbooks;
    private final List<DataSource> dataSources;
    private final Map<String, List<RecordSet>> recordSets;
    private final List<String> projectRecordEntities;
    private final List<String> reloadedEntities;
    private final Set<Entity> childEntities;
    private final Map<String, List<? extends Record>> projectRecords;
    private final Map<MultiKey, Set<String>> mismatchedExpeditions;

    public DatasetBuilder(DataReaderFactory dataReaderFactory, DataConverterFactory dataConverterFactory, RecordRepository repository,
                          Project project, String expeditionCode) {
        this.dataReaderFactory = dataReaderFactory;
        this.dataConverterFactory = dataConverterFactory;
        this.recordRepository = repository;
        this.project = project;
        this.config = project.getProjectConfig();
        this.expeditionCode = expeditionCode;

        this.workbooks = new ArrayList<>();
        this.dataSources = new ArrayList<>();
        this.recordSets = new HashMap<>();
        this.reloadedEntities = new ArrayList<>();
        this.childEntities = new HashSet<>();
        this.projectRecords = new HashMap<>();
        this.mismatchedExpeditions = new HashMap<>();

        this.projectRecordEntities = config.entities().stream()
                .filter(Entity::getUniqueAcrossProject)
                .map(Entity::getConceptAlias)
                .collect(Collectors.toList());
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

    public void addRecordSet(RecordSet recordSet) {
        String expeditionCode = (this.expeditionCode == null)
                ? recordSet.expeditionCode()
                : verifyExpeditionCode(recordSet);

        if (expeditionCode == null) {
            throw new FimsRuntimeException(ValidationCode.INVALID_DATASET, 400, "data is missing an expeditionCode");
        }

        add(recordSet);
    }


    public DatasetBuilder reloadWorkbooks(boolean reload) {
        this.reloadWorkbooks = reload;
        return this;
    }

    public Map<MultiKey, Set<String>> mismatchedExpeditions() {
        return mismatchedExpeditions;
    }

    public Dataset build() {

        instantiateWorkbookRecords();
        instantiateDataSourceRecords();
        mergeProjectRecords();
        fetchAndMergeParentRecords();
        setRecordSetParent();
        runDataConverters();

        if (recordSets.isEmpty()) {
            throw new FimsRuntimeException(ValidationCode.EMPTY_DATASET, 400);
        }

        return new Dataset(
                recordSets.values().stream().flatMap(List::stream).collect(Collectors.toList())
        );
    }

    private void instantiateDataSourceRecords() {
        for (DataSource dataSource : dataSources) {
            readData(dataSource.dataFile, dataSource.metadata, false);
        }
    }

    private void instantiateWorkbookRecords() {
        for (String file : workbooks) {
            readData(file, new RecordMetadata(TabularDataReaderType.READER_TYPE, reloadWorkbooks), true);
        }
    }

    private void readData(String file, RecordMetadata metadata, boolean isWorkbook) {
        DataReader reader = dataReaderFactory.getReader(file, config, metadata);

        // getRecordSets may return RecordSets with different expeditions but same entity
        for (RecordSet r : reader.getRecordSets()) {
            String expeditionCode = (this.expeditionCode == null)
                    ? r.expeditionCode()
                    : verifyExpeditionCode(r);

            if (expeditionCode == null) {
                String msg;
                if (isWorkbook) {
                    msg = "Data on the excel worksheet: \"" + r.entity().getWorksheet() + "\" is missing an expeditionCode";
                } else {
                    msg = "Data on the worksheet: \"" + r.entity().getWorksheet() + "\" is missing an expeditionCode";
                }
                throw new FimsRuntimeException(ValidationCode.INVALID_DATASET, 400, msg);
            }

            r.setProjectId(project.getProjectId());

            add(r);
        }

    }

    private void add(RecordSet recordSet) {
        recordSets.computeIfAbsent(recordSet.conceptAlias(), k -> new ArrayList<>()).add(recordSet);

        if (recordSet.projectId() == 0) {
            recordSet.setProjectId(project.getProjectId());
        }
        if (this.expeditionCode != null && recordSet.expeditionCode() == null) {
            recordSet.setExpeditionCode(this.expeditionCode);
        }

        Entity e = recordSet.entity();

        if (recordSet.reload()) {
            reloadedEntities.add(e.getConceptAlias());
        }
        if (e.isChildEntity()) childEntities.add(e);
    }

    private String verifyExpeditionCode(RecordSet recordSet) {
        String expeditionCode = recordSet.expeditionCode();
        if (expeditionCode != null && !expeditionCode.equals("") && !this.expeditionCode.equals(expeditionCode)) {
            this.mismatchedExpeditions.computeIfAbsent(
                    new MultiKey(recordSet.conceptAlias(), recordSet.entity().getWorksheet()),
                    k -> new HashSet<>()
            ).add(expeditionCode);
        }
        expeditionCode = this.expeditionCode;
        recordSet.setExpeditionCode(expeditionCode);
        return expeditionCode;
    }

    private void mergeProjectRecords() {
        for (String conceptAlias : projectRecordEntities) {
            if (!recordSets.containsKey(conceptAlias)) continue;

            // get expeditionCodes for RecordSets that we are reloading so we can exclude them from the project records
            List<String> expeditionCodes = recordSets.get(conceptAlias).stream()
                    .filter(RecordSet::reload)
                    .map(RecordSet::expeditionCode)
                    .collect(Collectors.toList());

            List<? extends Record> records = getProjectRecords(recordSets.get(conceptAlias).get(0).entity()).stream()
                    .filter(r -> !expeditionCodes.contains(r.expeditionCode()))
                    .collect(Collectors.toList());

            for (RecordSet r : recordSets.get(conceptAlias)) {
                if (!r.isEmpty()) {
                    r.merge(records);
                }
            }
        }
    }

    private void fetchAndMergeParentRecords() {
        Set<Entity> parentEntities = getChildRecordSetParentEntities();

        for (Entity e : parentEntities) {

            if (fetchRecordSet(e.getConceptAlias())) {
                Map<String, List<Record>> projectRecords = new HashMap<>();

                if (expeditionCode != null) {
                    List<Record> records = projectRecordEntities.contains(e.getConceptAlias())
                            ? getProjectRecords(e).stream()
                            .filter(r -> Objects.equals(r.expeditionCode(), expeditionCode))
                            .collect(Collectors.toList())
                            : (List<Record>) recordRepository.getRecords(project, expeditionCode, e.getConceptAlias(), e.getRecordType());
                    projectRecords.put(expeditionCode, records);

                } else {
                    projectRecords.putAll(
                            getProjectRecords(e).stream()
                                    .collect(Collectors.groupingBy(Record::expeditionCode))
                    );
                }

                getParentRecordSets(e).stream()
                        .filter(r -> !r.reload())
                        .forEach(r -> r.merge(
                                projectRecords.getOrDefault(r.expeditionCode(), Collections.emptyList())
                        ));
            }
        }
    }

    private List<RecordSet> getParentRecordSets(Entity e) {
        // for each child expedition, we need a parent recordSet
        List<String> childExpeditions = expeditionCode != null
                ? new ArrayList<>(Collections.singletonList(expeditionCode))
                : this.childEntities.stream()
                .filter(entity -> e.getConceptAlias().equals(entity.getParentEntity()))
                .flatMap(entity -> recordSets.get(entity.getConceptAlias()).stream())
                .map(RecordSet::expeditionCode)
                .distinct()
                .collect(Collectors.toList());

        List<RecordSet> parentRecordSets = this.recordSets.computeIfAbsent(e.getConceptAlias(), k -> new ArrayList<>());

        // remove expeditions we already have a record set for
        parentRecordSets.forEach(r -> childExpeditions.remove(r.expeditionCode()));

        // add any missing parent recordSets
        childExpeditions.forEach(expeditionCode -> {
            RecordSet r = new RecordSet(e, false);
            r.setExpeditionCode(expeditionCode);
            parentRecordSets.add(r);
        });
        return parentRecordSets;
    }

    private List<? extends Record> getProjectRecords(Entity e) {
        return projectRecords.computeIfAbsent(
                e.getConceptAlias(),
                k -> recordRepository.getRecords(project, e.getConceptAlias(), e.getRecordType())
        );
    }

    // note this will not fetch grandParents. Currently we only fetch the parent
    // recordSet of any record that is being updated. We should not need to
    // fetch the entire hierarchy
    private Set<Entity> getChildRecordSetParentEntities() {
        Set<Entity> parentEntities = new HashSet<>();

        for (Entity child : childEntities) {
            parentEntities.add(config.entity(child.getParentEntity()));
        }

        return parentEntities;
    }

    private boolean fetchRecordSet(String conceptAlias) {
        return !recordSets.containsKey(conceptAlias)
                || recordSets.get(conceptAlias).stream().anyMatch(r -> !r.reload());
    }

    private void setRecordSetParent() {
        // we have to process parents first b/c we may replace the RecordSet and we want the parent() to be the most
        // up-to-date RecordSet
        for (Entity e : config.entities(EntitySort.PARENTS_FIRST)) {
            if (!childEntities.contains(e)) continue;

            List<RecordSet> recordSetsToRemove = new ArrayList<>();
            List<RecordSet> recordSetsToAdd = new ArrayList<>();

            for (RecordSet r : recordSets.get(e.getConceptAlias())) {
                RecordSet parentRecordSet = getRecordSet(e.getParentEntity(), r.expeditionCode());
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

                    if (newRecordSet.isEmpty()) {
                        // we set these here incase the RecordSet is empty. We may need this information
                        // in downstream processing
                        newRecordSet.setExpeditionCode(r.expeditionCode());
                        newRecordSet.setProjectId(r.projectId());
                    }

                    newRecordSet.setParent(parentRecordSet);
                    recordSetsToAdd.add(newRecordSet);
                    recordSetsToRemove.add(r);
                } else {
                    r.setParent(parentRecordSet);
                }
            }

            recordSets.get(e.getConceptAlias()).removeAll(recordSetsToRemove);
            recordSets.get(e.getConceptAlias()).addAll(recordSetsToAdd);
        }
    }

    private void runDataConverters() {
        for (Entity e : config.entities(EntitySort.PARENTS_FIRST)) {
            for (RecordSet r : recordSets.getOrDefault(e.getConceptAlias(), Collections.emptyList())) {
                DataConverter converter = dataConverterFactory.getConverter(e.type(), config);
                converter.convertRecordSet(r, project.getNetwork().getId());
            }
        }
    }

    private RecordSet getRecordSet(String conceptAlias, String expeditionCode) {
        return recordSets.getOrDefault(conceptAlias, new ArrayList<>()).stream()
                .filter(r -> Objects.equals(r.expeditionCode(), expeditionCode))
                .findFirst()
                .orElse(new RecordSet(config.entity(conceptAlias), false));
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
