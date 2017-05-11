package biocode.fims.reader.plugins;

import biocode.fims.digester.Attribute;
import biocode.fims.digester.Entity;
import biocode.fims.fimsExceptions.FimsRuntimeException;
import biocode.fims.models.records.Record;
import biocode.fims.models.records.RecordMetadata;
import biocode.fims.models.records.RecordSet;
import biocode.fims.projectConfig.ProjectConfig;
import biocode.fims.reader.DataReader;
import biocode.fims.reader.TabularDataReaderType;
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
                    new RecordSet(e.getKey(), e.getValue())
            );
        }

        return recordSets;
    }

    void instantiateRecordsFromRow(LinkedList<String> row) {
        if (addRow(row)) {

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

                    entityRecords.computeIfAbsent(e, k -> new ArrayList<>()).add(r);

                } catch (InstantiationException | IllegalAccessException e1) {
                    throw new FimsRuntimeException("", 500);
                }
            }

        }
    }

    private boolean addRow(List<String> row) {
        return row.size() > 0 &&
                row.stream()
                        .filter(s -> !StringUtils.isBlank(s))
                        .count() > 0;
    }

    abstract void init();
    abstract void instantiateRecords();

    @Override
    public DataReaderType readerType() {
        return TabularDataReaderType.READER_TYPE;
    }
}
