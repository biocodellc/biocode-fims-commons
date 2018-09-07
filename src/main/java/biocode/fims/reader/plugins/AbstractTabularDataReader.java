package biocode.fims.reader.plugins;

import biocode.fims.config.models.Attribute;
import biocode.fims.config.models.Entity;
import biocode.fims.config.project.ProjectConfig;
import biocode.fims.fimsExceptions.FimsRuntimeException;
import biocode.fims.records.Record;
import biocode.fims.records.RecordMetadata;
import biocode.fims.records.RecordSet;
import biocode.fims.reader.DataReader;
import biocode.fims.reader.TabularDataReaderType;
import biocode.fims.utils.RecordHasher;
import org.apache.commons.lang3.StringUtils;
import org.springframework.util.Assert;

import java.io.File;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author rjewing
 */
abstract class AbstractTabularDataReader implements DataReader {
    protected File file;
    protected ProjectConfig config;
    private RecordMetadata recordMetadata;
    protected Map<Entity, List<Record>> entityRecords;
    protected List<Entity> sheetEntities;
    protected List<String> colNames;

    private List<RecordSet> recordSets;
    private boolean sortedSheetEntites = false;
    private boolean foundExpeditionCode = false;

    AbstractTabularDataReader() {
    }

    AbstractTabularDataReader(File file, ProjectConfig config, RecordMetadata recordMetadata) {
        Assert.notNull(file);
        Assert.notNull(config);
        Assert.notNull(recordMetadata);
        this.file = file;
        this.config = config;
        this.recordMetadata = recordMetadata;
        this.entityRecords = new HashMap<>();
    }

    @Override
    public List<RecordSet> getRecordSets() {

        if (recordSets == null) {
            init();
            instantiateRecords();
            recordSets = generateRecordSets();
        }

        return recordSets;
    }

    private List<RecordSet> generateRecordSets() {
        List<RecordSet> recordSets = new ArrayList<>();

        for (Map.Entry<Entity, List<Record>> e : entityRecords.entrySet()) {

            // group records by expeditionCode
            Map<Optional<String>, List<Record>> grouped = e.getValue().stream()
                    .collect(Collectors.groupingBy(r -> Optional.ofNullable(r.expeditionCode())));

            for (Map.Entry<Optional<String>, List<Record>> ge : grouped.entrySet()) {
                RecordSet recordSet = new RecordSet(e.getKey(), ge.getValue(), recordMetadata.reload());
                if (ge.getKey().isPresent()) {
                    recordSet.setExpeditionCode(ge.getKey().get());
                }
                recordSets.add(recordSet);
            }
        }

        return recordSets;
    }

    void instantiateRecordsFromRow(LinkedList<String> row) {
        if (addRow(row)) {
            // get the expeditionCode if the data has it
            String expeditionCode = (colNames.contains(Record.EXPEDITION_CODE))
                    ? row.get(colNames.indexOf(Record.EXPEDITION_CODE))
                    : null;

            if (!foundExpeditionCode && expeditionCode != null) {
                foundExpeditionCode = true;
            }

            // this ensures that parent entities come before children
            // so we can update the child parent identifier for hashed
            // entices
            if (!sortedSheetEntites) sortSheetEntities();

            // hashed Records for the row
            Map<String, Record> hashedRecords = new HashMap<>();

            for (Entity e : sheetEntities) {
                try {
                    Record r = e.getRecordType().newInstance();
                    r.setMetadata(recordMetadata);
                    r.setExpeditionCode(expeditionCode);

                    for (Attribute a : e.getAttributes()) {
                        if (colNames.contains(a.getColumn())) {
                            String val = row.get(
                                    colNames.indexOf(a.getColumn())
                            );

                            r.set(a.getUri(), val);
                        }
                    }

                    if (e.isHashed()) {
                        String uniqueKey = RecordHasher.hash(r);
                        r.set(e.getUniqueKeyURI(), uniqueKey);
                        hashedRecords.put(e.getConceptAlias(), r);
                    }

                    // if this is a child and the parent entity is hashed, set the parent identifier
                    if (e.isChildEntity() && hashedRecords.containsKey(e.getParentEntity())) {
                        String parentUniqueKeyUri = config.entity(e.getParentEntity()).getUniqueKeyURI();
                        Record parentRecord = hashedRecords.get(e.getParentEntity());
                        r.set(parentUniqueKeyUri, parentRecord.get(parentUniqueKeyUri));
                    }

                    entityRecords.computeIfAbsent(e, k -> new ArrayList<>()).add(r);

                } catch (InstantiationException | IllegalAccessException e1) {
                    throw new FimsRuntimeException("", 500);
                }
            }
        }
    }

    /**
     * sorts sheetEntities so parent entities come before children
     */
    private void sortSheetEntities() {
        sheetEntities.sort((a, b) -> {
            if (a.isChildEntity()) {
                if (b.isChildEntity()) {
                    return config.isEntityChildDescendent(b, a) ? 1 : -1;
                }
                return 1;
            } else if (b.isChildEntity()) {
                return -1;
            }
            return 0;
        });
    }

    private boolean addRow(List<String> row) {
        return row.size() > 0 &&
                row.stream()
                        .anyMatch(s -> !StringUtils.isBlank(s));
    }

    abstract void init();

    abstract void instantiateRecords();

    @Override
    public DataReaderType readerType() {
        return TabularDataReaderType.READER_TYPE;
    }
}
