package biocode.fims.query.writers;

import biocode.fims.config.Config;
import biocode.fims.fimsExceptions.FimsRuntimeException;
import biocode.fims.fimsExceptions.errorCodes.QueryCode;
import biocode.fims.config.project.ColumnComparator;
import biocode.fims.config.models.Attribute;
import biocode.fims.config.models.Entity;
import biocode.fims.query.QueryResult;
import biocode.fims.query.QueryResults;
import org.apache.commons.collections.keyvalue.MultiKey;

import java.util.*;
import java.util.stream.Collectors;

class WriterSheetGenerator {
    private final QueryResults queryResults;
    private final Config config;
    private final Map<String, SheetRecords> recordsBySheet;
    private final Map<String, List<Entity>> entitiesBySheet;

    private String currentSheet;
    private QueryResult currentResult;

    WriterSheetGenerator(QueryResults queryResults, Config config) {
        this.queryResults = queryResults;
        this.config = config;
        recordsBySheet = new HashMap<>();
        entitiesBySheet = new HashMap<>();
    }

    List<WriterWorksheet> recordsToWriterSheets() {

        for (Map.Entry<String, LinkedList<QueryResult>> entry : queryResultsBySheet().entrySet()) {
            currentSheet = entry.getKey();
            System.out.println(config.getClass());
            System.out.println(config);
            System.out.println(currentSheet);

            for (QueryResult queryResult : entry.getValue()) {
                currentResult = queryResult;
                entitiesBySheet.computeIfAbsent(currentSheet, k -> new ArrayList<>()).add(currentResult.entity());
                System.out.println(currentResult.entity().getConceptAlias());
                System.out.println(config.entitiesForSheet(currentSheet).stream().map(Entity::getConceptAlias).collect(Collectors.joining(",")));

                if (recordsBySheet.containsKey(currentSheet)) {
                    mergeSheetRecords();
                } else {
                    recordsBySheet.put(currentSheet, new SheetRecords(config.entitiesForSheet(currentSheet), currentResult));
                }
            }
        }

        return generateWriterWorksheets();
    }

    /**
     * get queryResults sorted by the worksheet they belong on
     * <p>
     * This will include multiple queryResults on the same worksheet only if it is
     * possible to join the results, otherwise they will be on separate worksheets
     *
     * @return
     */
    private Map<String, LinkedList<QueryResult>> queryResultsBySheet() {
        Map<String, LinkedList<QueryResult>> sheetResults = new HashMap<>();

        // sort queryResults so children come first
        queryResults.sort(new QueryResults.ChildrenFirstComparator(config));

        // map queryResults by worksheet
        for (QueryResult queryResult : queryResults) {
            Entity e = queryResult.entity();

            if (!e.hasWorksheet()) continue;

            String worksheet = e.getWorksheet();

            if (sheetResults.containsKey(worksheet)) {
                Entity e2 = sheetResults.get(worksheet).getLast().entity();

                if (e2.isChildEntity() && e2.getParentEntity().equals(e.getConceptAlias())) {
                    // previous entity was a child of this entity
                    sheetResults.get(worksheet).add(queryResult);
                } else {
                    // we can't join these entities, so we need a new sheet
                    sheetResults.put(worksheet + "_" + e.getConceptAlias(), new LinkedList<>(Collections.singletonList(queryResult)));
                }
            } else {
                sheetResults.put(worksheet, new LinkedList<>(Collections.singletonList(queryResult)));
            }
        }

        return sheetResults;
    }

    /**
     * merge queryResults with an existing sheet.
     */
    private void mergeSheetRecords() {
        String conceptAlias = currentResult.entity().getConceptAlias();
        LinkedList<Map<String, String>> records = currentResult.get(false);
        try {
            recordsBySheet.get(currentSheet).addRecords(conceptAlias, records);
        } catch (FimsRuntimeException exp) {
            if (exp.getErrorCode().equals(QueryCode.INVALID_JOIN)) {
                // need to create a new sheet
                recordsBySheet.put(
                        currentSheet + "_" + conceptAlias,
                        new SheetRecords(config.entitiesForSheet(currentSheet), conceptAlias, records));
                return;
            }

            throw exp;
        }
    }

    private List<WriterWorksheet> generateWriterWorksheets() {
        List<WriterWorksheet> sheets = new ArrayList<>();
        for (String sheetName : recordsBySheet.keySet()) {
            List<Map<String, String>> records = recordsBySheet.get(sheetName).records();


            LinkedList<String> columns = new LinkedList<>(config.attributesForSheet(sheetName)
                    .stream()
                    .map(Attribute::getColumn)
                    .collect(Collectors.toSet()));

            records.stream()
                    .flatMap(r -> r.keySet().stream())
                    .forEach(c -> {
                        if (!columns.contains(c)) {
                            columns.add(c);
                        }
                    });

            if (config.entitiesForSheet(sheetName).size() > 0) {
                columns.sort(new ColumnComparator(config, sheetName));
            }

            List<String> hashedEntitiesKeys = entitiesBySheet.getOrDefault(sheetName, Collections.emptyList()).stream()
                    .filter(Entity::isHashed)
                    .map(Entity::getUniqueKey)
                    .collect(Collectors.toList());

            // remove any auto-generated keys
            if (hashedEntitiesKeys.size() > 0) {
                columns.removeAll(hashedEntitiesKeys);
                for (Map<String, String> r : records) {
                    for (String k : hashedEntitiesKeys) {
                        r.remove(k);
                    }
                }
            }

            sheets.add(
                    new WriterWorksheet(sheetName, columns, records)
            );
        }

        return sheets;
    }

    private static class SheetRecords {
        private final HashMap<String, Entity> sheetEntities;

        private List<Map<String, String>> records;
        private Map<MultiKey, List<Map<String, String>>> recordKeys;
        private String firstEntity;
        private boolean replacedBcid = false;

        SheetRecords(List<Entity> sheetEntities) {
            this.sheetEntities = new HashMap<>();
            sheetEntities.forEach(e -> this.sheetEntities.put(e.getConceptAlias(), e));
            records = new ArrayList<>();
            recordKeys = new HashMap<>();
        }

        SheetRecords(List<Entity> sheetEntities, QueryResult queryResult) {
            this(sheetEntities);
            addRecords(queryResult.entity().getConceptAlias(), queryResult.get(false));
        }

        SheetRecords(List<Entity> sheetEntities, String conceptAlias, List<Map<String, String>> records) {
            this(sheetEntities);
            addRecords(conceptAlias, records);
        }

        /**
         * The following assumptions are made when adding records...
         * <p>
         * 1. called in order of entity relation, with most atomic entity coming first
         * 2. records to add are a direct parent-child relation with previously added records.
         * sibling relationships are not supported.
         *
         * @param conceptAlias
         * @param records
         */
        void addRecords(String conceptAlias, List<Map<String, String>> records) {
            Entity e = sheetEntities.get(conceptAlias);
            String uniqueKey = e.getUniqueKey();

            if (this.records.isEmpty()) {
                records.forEach(r -> {
                    // b/c first record should be most atomic entity, we don't need to add recordKey for this record
                    this.records.add(r);

                    if (e.isChildEntity() && sheetEntities.containsKey(e.getParentEntity())) {
                        String parentUniqueKey = sheetEntities.get(e.getParentEntity()).getUniqueKey();
                        MultiKey pk = new MultiKey(e.getParentEntity(), r.get("projectId"), r.get("expeditionCode"), r.get(parentUniqueKey));
                        this.recordKeys.computeIfAbsent(pk, x -> new ArrayList<>()).add(r);
                    }
                });
                firstEntity = conceptAlias;
                return;
            }

            // for each record, join to any existing records that match the key (conceptAlias, projectId, expeditionCode, uniqueKey)
            // otherwise add to records list
            for (Map<String, String> r : records) {
                MultiKey k = new MultiKey(conceptAlias, r.get("projectId"), r.get("expeditionCode"), r.get(uniqueKey));

                if (!e.isHashed()) {
                    r.put(e.getConceptAlias() + "_bcid", r.get("bcid"));
                }
                r.remove("bcid");

                List<Map<String, String>> keyedRecords = new ArrayList<>();

                // not all parent records have a child record
                if (!this.recordKeys.containsKey(k)) {
                    this.records.add(r);
                    keyedRecords.add(r);
                } else {
                    // append the record properties for any existing records that match the key
                    for (Map<String, String> existingRecord : recordKeys.get(k)) {
                        existingRecord.putAll(r);
                        keyedRecords.add(existingRecord);
                    }
                }

                if (e.isChildEntity() && sheetEntities.containsKey(e.getParentEntity())) {
                    String parentUniqueKey = sheetEntities.get(e.getParentEntity()).getUniqueKey();
                    MultiKey pk = new MultiKey(e.getParentEntity(), r.get("projectId"), r.get("expeditionCode"), r.get(parentUniqueKey));
                    recordKeys.computeIfAbsent(pk, x -> new ArrayList<>()).addAll(keyedRecords);
                }
            }

            // rename and update the existing bcid if needed
            if (!replacedBcid) {
                this.records.forEach(er -> {
                    String bcid = er.remove("bcid");
                    if (bcid != null) {
                        er.put(firstEntity + "_bcid", bcid);
                    }
                });
                replacedBcid = true;
            }
        }

        List<Map<String, String>> records() {
            return records;
        }
    }
}