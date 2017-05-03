package biocode.fims.reader.plugins;

import biocode.fims.digester.Attribute;
import biocode.fims.digester.Entity;
import biocode.fims.digester.Mapping;
import biocode.fims.fimsExceptions.FimsRuntimeException;
import biocode.fims.models.records.Record;
import biocode.fims.models.records.RecordMetadata;
import biocode.fims.models.records.RecordSet;
import org.apache.commons.lang.StringUtils;
import org.springframework.util.Assert;

import java.io.File;
import java.util.*;

/**
 * @author rjewing
 */
abstract class AbstractTabularDataReader implements DataReader {
    protected File file;
    protected Mapping mapping;
    private RecordMetadata recordMetadata;
    protected Map<String, List<Record>> entityRecords;
    protected List<Entity> sheetEntities;
    protected List<String> colNames;

    AbstractTabularDataReader() {
    }

    AbstractTabularDataReader(File file, Mapping mapping, RecordMetadata recordMetadata) {
        Assert.notNull(file);
        Assert.notNull(mapping);
        Assert.notNull(recordMetadata);
        this.file = file;
        this.mapping = mapping;
        this.recordMetadata = recordMetadata;
        this.entityRecords = new HashMap<>();
    }

    @Override
    public List<RecordSet> getRecordSets() {

        init();
        instantiateRecords();

        return generateRecordSets();
    }

    private List<RecordSet> generateRecordSets() {
        List<RecordSet> recordSets = new ArrayList<>();

        for (Map.Entry<String, List<Record>> e : entityRecords.entrySet()) {
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
                    Record r = recordMetadata.type().newInstance();

                    for (Attribute a : e.getAttributes()) {
                        if (colNames.contains(a.getColumn())) {
                            String val = row.get(
                                    colNames.indexOf(a.getColumn())
                            );

                            r.set(a.getUri(), val);
                        }
                    }

                    entityRecords.computeIfAbsent(e.getConceptAlias(), k -> new ArrayList<>()).add(r);

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
}
