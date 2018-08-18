package biocode.fims.query.writers;

import biocode.fims.projectConfig.ColumnComparator;
import biocode.fims.projectConfig.ProjectConfig;
import biocode.fims.projectConfig.models.Attribute;
import biocode.fims.projectConfig.models.Entity;
import biocode.fims.query.QueryResult;
import biocode.fims.query.QueryResults;

import java.util.*;
import java.util.stream.Collectors;

class WriterSheetGenerator {
    private final QueryResults queryResults;
    private final ProjectConfig config;
    private final Map<String, List<Map<String, String>>> recordsBySheet;
    private final Map<String, List<Entity>> entitiesBySheet;

    private String currentSheet;
    private QueryResult currentResult;

    WriterSheetGenerator(QueryResults queryResults, ProjectConfig config) {
        this.queryResults = queryResults;
        this.config = config;
        recordsBySheet = new HashMap<>();
        entitiesBySheet = new HashMap<>();
    }

    List<WriterWorksheet> recordsToWriterSheets() {

        // sort entities so children come first
        queryResults.sort(queryResultComparator());

        for (QueryResult queryResult : queryResults) {
            currentSheet = queryResult.entity().getWorksheet();

            // this can be the case when trying to output fasta data as a csv
            if (currentSheet == null) {
                currentSheet = queryResult.entity().getConceptAlias();
            }

            currentResult = queryResult;

            if (recordsBySheet.containsKey(currentSheet)) {
                addQueryResult();
            } else {
                recordsBySheet.put(currentSheet, new ArrayList<>(currentResult.get(false)));
                entitiesBySheet.computeIfAbsent(currentSheet, k -> new ArrayList<>()).add(currentResult.entity());
            }

        }

        return generateWriterWorksheets();
    }

    private void addQueryResult() {
        List<String> commonColumns = getCommonColumns(currentResult.entity());

        entitiesBySheet.get(currentSheet).add(currentResult.entity());

        // if we don't have any common columns, then there is no way to join the records
        if (commonColumns.size() == 0) {
            // we need to put all records on a new currentSheet
            recordsBySheet.put(currentSheet + "_" + currentResult.entity().getConceptAlias(), currentResult.get(false));
            return;
        }

        mergeQueryResult(commonColumns);
    }

    /**
     * attempt to merge queryResults to an existing sheet. If we can't join the records, then we create a new sheet
     *
     * @param commonColumns
     */
    private void mergeQueryResult(List<String> commonColumns) {
        // tmp holding variable for merged records. This is needed b/c if we find
        // just 1 non-match, we need to put everything on a new currentSheet
        // so we store our matches in mergedRecords and replace existingRecords w/ mergedRecords
        // once we know that we find a match for everything
        List<Map<String, String>> mergedRecords = new ArrayList<>();
        List<Map<String, String>> matchedRecords = new ArrayList<>();

        // For each record, try and find an existingRecord where all commonColumns match
        // if there is only 1 existingRecord where all commonColumns match, then we can
        // do the join
        boolean newSheet = false;
        for (Map<String, String> record : currentResult.get(false)) {

            // TODO what happens when event,sample,tissue are on same currentSheet, and only event & tissue are in queryResults?
            boolean foundMatch = false;

            for (Map<String, String> existing : recordsBySheet.get(currentSheet)) {
                boolean matches = true;

                // check if we can join record and existing
                for (String col : commonColumns) {
                    if (!Objects.equals(existing.get(col), record.get(col))) {
                        matches = false;
                        break;
                    }
                }

                // b/c we sort queryResults by entity atomicity (children first)
                // we can append the record properties to all existing records that match
                if (matches) {
                    Entity e = currentResult.entity();
                    if (e.isHashed()) {
                        record.remove("bcid");
                    } else {
                        record.put(e.getConceptAlias() + "_bcid", record.get("bcid"));
                        record.remove("bcid");

                        // rename an existing bcid if needed
                        if (existing.get("bcid") != null) {
                            List<Entity> sheetEntities = entitiesBySheet.get(currentSheet).stream()
                                    .filter(en -> !en.getConceptAlias().equals(e.getConceptAlias()))
                                    .collect(Collectors.toList());

                            // update bcid key if we can determine what entity it is for
                            if (sheetEntities.size() == 1) {
                                existing.put(sheetEntities.get(0).getConceptAlias() + "_bcid", existing.get("bcid"));
                                existing.remove("bcid");
                            }
                        }
                    }


                    matchedRecords.add(existing);
                    HashMap<String, String> merged = new HashMap<>(existing);
                    merged.putAll(record);
                    mergedRecords.add(merged);
                    foundMatch = true;
                }
            }

            if (!foundMatch) {
                // we are done here. all records need to be placed on a new currentSheet
                newSheet = true;
                break;
            }
        }

        if (newSheet) {
            recordsBySheet.put(currentSheet + "_" + currentResult.entity().getConceptAlias(), currentResult.get(false));
        } else {
            List<Map<String, String>> records = recordsBySheet.get(currentSheet);
            records.removeAll(matchedRecords);
            records.addAll(mergedRecords);
        }
    }

    private List<WriterWorksheet> generateWriterWorksheets() {
        List<WriterWorksheet> sheets = new ArrayList<>();
        for (String sheetName : recordsBySheet.keySet()) {
            List<Map<String, String>> records = recordsBySheet.get(sheetName);

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

            if (config.entitiesForSheet(currentSheet).size() > 0) {
                columns.sort(new ColumnComparator(config, currentSheet));
            }

            sheets.add(
                    new WriterWorksheet(sheetName, columns, records)
            );
        }

        return sheets;
    }

    /**
     * Comparator to sort child entities before parents
     *
     * @return
     */
    private Comparator<QueryResult> queryResultComparator() {
        return (a, b) -> {
            if (!a.entity().getWorksheet().equals(b.entity().getWorksheet())) return 0;

            Entity e1 = a.entity();
            Entity e2 = b.entity();

            if (e1.isChildEntity() && e2.getConceptAlias().equals(e1.getParentEntity())) return -1;
            if (e2.isChildEntity() && e1.getConceptAlias().equals(e1.getParentEntity())) return 1;

            return 0;
        };
    }

    private List<String> getCommonColumns(Entity entity) {
        List<String> existingColumns = entitiesBySheet.get(currentSheet).stream()
                .flatMap(e -> e.getAttributes().stream())
                .map(Attribute::getColumn)
                .collect(Collectors.toList());

        return entity.getAttributes().stream()
                .map(Attribute::getColumn)
                .filter(existingColumns::contains)
                .collect(Collectors.toList());
    }
}