package biocode.fims.reader.plugins;

import biocode.fims.projectConfig.models.Attribute;
import biocode.fims.projectConfig.models.Entity;
import biocode.fims.fimsExceptions.FimsRuntimeException;
import biocode.fims.models.records.Record;
import biocode.fims.models.records.RecordMetadata;
import biocode.fims.models.records.RecordSet;
import biocode.fims.projectConfig.ProjectConfig;
import biocode.fims.reader.DataReader;
import biocode.fims.reader.TabularDataReaderType;
import biocode.fims.utils.RecordHasher;
import org.apache.commons.lang.StringUtils;
import org.springframework.util.Assert;

import java.io.File;
import java.util.*;

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
            recordSets.add(
                    new RecordSet(e.getKey(), e.getValue(), recordMetadata.reload())
            );
        }

        return recordSets;
    }

    void instantiateRecordsFromRow(LinkedList<String> row) {
        if (addRow(row)) {

            // this ensures that parent entities come before children
            // so we can update the child parent identifier for hashed
            // entices
            if (!sortedSheetEntites) sortSheetEntities();

            Map<String, Record> hashedRecords = new HashMap<>();

            for (Entity e : sheetEntities) {
                try {
                    Record r = e.getRecordType().newInstance();
                    r.setMetadata(recordMetadata);

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
