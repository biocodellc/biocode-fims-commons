package biocode.fims.records;

import biocode.fims.config.Config;
import biocode.fims.fimsExceptions.FimsRuntimeException;
import biocode.fims.fimsExceptions.errorCodes.QueryCode;
import biocode.fims.config.models.Attribute;
import biocode.fims.config.models.Entity;
import biocode.fims.query.QueryResult;
import biocode.fims.query.QueryResults;

import java.util.*;
import java.util.function.Function;

public class RecordJoiner {
    private final QueryResults queryResults;
    private final Entity recordEntity;
    private final Map<String, Map<String, Record>> cache;

    public RecordJoiner(Config config, Entity recordEntity, QueryResults queryResults) {
        this.recordEntity = recordEntity;
        this.queryResults = queryResults;
        this.cache = new HashMap<>();

        // sort entities so children come first
        this.queryResults.sort(new QueryResults.ChildrenFirstComparator(config));
    }

    /**
     * join all ancestral data in queryResults into a single map
     * <p>
     * note: this will overwrite attributes in separate entities that
     * have the same {@link Attribute#uri}
     * with the eldest (in the relationship) entities record
     * <p>
     * The rootIdentifier for each record join will be added to the
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

            String rootIdentifierKey = queryResult.entity().getConceptAlias() + "_rootIdentifier";

            Map<String, Record> records = cache.computeIfAbsent(
                    queryResult.entity().getConceptAlias(),
                    cacheBuilder(queryResult, joinKey)
            );

            Record r = records.get(joinValue);
            data.putAll(r.properties());
            data.put(rootIdentifierKey, r.rootIdentifier());
        }
        data.putAll(record.properties());

        return new GenericRecord(data, record.rootIdentifier(), record.projectId(), record.expeditionCode(), false);
    }

    /**
     * Fetch the parent record for the child
     *
     * @param conceptAlias Entity of the parent record we want
     * @param record       Child record to find the parent for
     * @return
     */
    public Record getParent(String conceptAlias, Record record) {
        Record r = record;

        for (QueryResult queryResult : queryResultsToJoinParent(conceptAlias)) {
            String joinKey = queryResult.entity().getUniqueKeyURI();
            String joinValue = r.get(joinKey);

            if (joinValue.equals("")) {
                joinValue = record.get(joinKey);
            }

            if (Objects.equals(joinValue, "")) {
                throw new FimsRuntimeException(QueryCode.MISSING_RECORD, 500, record.get(recordEntity.getUniqueKeyURI()));
            }

            Map<String, Record> records = cache.computeIfAbsent(
                    queryResult.entity().getConceptAlias(),
                    cacheBuilder(queryResult, joinKey)
            );

            r = records.get(joinValue);
        }

        if (r.equals(record)) {
            throw new FimsRuntimeException(QueryCode.MISSING_RECORD, 500);
        }

        return r;
    }

    private List<QueryResult> queryResultsToJoinParent(String conceptAlias) {
        List<QueryResult> results = new LinkedList<>();

        boolean foundRecordEntity = false;
        for (QueryResult queryResult : queryResults.results()) {
            if (queryResult.entity().equals(recordEntity)) {
                foundRecordEntity = true;
            } else if (foundRecordEntity) {
                results.add(queryResult);
                if (queryResult.entity().getConceptAlias().equals(conceptAlias)) break;
            }
        }

        return results;
    }

    private Function<String, Map<String, Record>> cacheBuilder(QueryResult queryResult, String joinKey) {
        return k -> {
            Map<String, Record> map = new HashMap<>();
            queryResult.records().forEach(r -> map.put(r.get(joinKey), r));
            return map;
        };
    }
}