package biocode.fims.records;

import biocode.fims.fimsExceptions.FimsRuntimeException;
import biocode.fims.fimsExceptions.errorCodes.QueryCode;
import biocode.fims.projectConfig.models.Attribute;
import biocode.fims.projectConfig.models.Entity;
import biocode.fims.query.QueryResult;
import biocode.fims.query.QueryResults;

import java.util.HashMap;
import java.util.Map;

public class RecordJoiner {
    private final QueryResults queryResults;
    private final Entity recordEntity;

    public RecordJoiner(Entity recordEntity, QueryResults queryResults) {
        this.recordEntity = recordEntity;
        this.queryResults = queryResults;
    }

    /**
     * join all ancestral data in queryResults into a single map
     * <p>
     * note: this will overwrite attributes in separate entities that
     * have the same {@link Attribute#uri}
     * with the eldest (in the relationship) entities record
     *
     * @param record The child record were should join data for
     */
    public Record joinRecords(Record record) {
        Map<String, String> data = new HashMap<>();

        for (QueryResult queryResult : queryResults.results()) {
            if (queryResult.entity().equals(recordEntity)) continue;

            String joinKey = queryResult.entity().getUniqueKeyURI();
            String joinValue = record.get(joinKey);

            if (joinValue.equals("")) {
                joinValue = data.get(joinKey);
            }

            if (joinValue == null) {
                throw new FimsRuntimeException(QueryCode.MISSING_RECORD, 500, record.get(recordEntity.getUniqueKeyURI()));
            }

            for (Record r : queryResult.records()) {
                if (r.get(joinKey).equals(joinValue)) {
                    data.putAll(r.properties());
                    break;
                }
            }
        }

        return new GenericRecord(data, null, record.projectId(), record.expeditionCode(), false);
    }
}