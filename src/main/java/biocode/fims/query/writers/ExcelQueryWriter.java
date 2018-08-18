package biocode.fims.query.writers;

import biocode.fims.projectConfig.models.Attribute;
import biocode.fims.projectConfig.models.Entity;
import biocode.fims.fimsExceptions.FimsRuntimeException;
import biocode.fims.fimsExceptions.errorCodes.QueryCode;
import biocode.fims.models.Project;
import biocode.fims.query.QueryResult;
import biocode.fims.query.QueryResults;
import biocode.fims.run.ExcelWorkbookWriter;

import java.io.File;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author RJ Ewing
 */
public class ExcelQueryWriter extends ExcelWorkbookWriter implements QueryWriter {
    private final QueryResults queryResults;

    public ExcelQueryWriter(Project project, QueryResults queryResults, int naan) {
        super(project, naan);
        this.queryResults = queryResults;
    }

    @Override
    public File write() {
        if (queryResults.isEmpty()) {
            throw new FimsRuntimeException(QueryCode.NO_RESOURCES, 400);
        }

        List<ExcelWorkbookWriter.WorkbookWriterSheet> sheets = recordsToWriterSheets();

        return super.write(sheets);
    }

    private List<ExcelWorkbookWriter.WorkbookWriterSheet> recordsToWriterSheets() {

        Map<String, List<Map<String, String>>> recordsBySheet = new HashMap<>();
        Map<String, List<Entity>> entitiesBySheet = new HashMap<>();

        // sort entities so children come first
        queryResults.sort((a, b) -> {
            if (!a.entity().getWorksheet().equals(b.entity().getWorksheet())) return 0;

            Entity e1 = a.entity();
            Entity e2 = b.entity();

            if (e1.isChildEntity() && e2.getConceptAlias().equals(e1.getParentEntity())) return -1;
            if (e2.isChildEntity() && e1.getConceptAlias().equals(e1.getParentEntity())) return 1;

            return 0;
        });

        for (QueryResult queryResult : queryResults) {
            String sheet = queryResult.entity().getWorksheet();

            if (recordsBySheet.containsKey(sheet)) {
                List<Map<String, String>> existingRecords = recordsBySheet.get(sheet);
                List<String> commonColumns = getCommonColumns(entitiesBySheet.get(sheet), queryResult.entity());
                // tmp holding variable for merged records. This is needed b/c if we find
                // just 1 non-match, we need to put everything on a new sheet
                // so we store our matches in mergedRecords and replace existingRecords w/ mergedRecords
                // once we know that we find a match for everything
                List<Map<String, String>> mergedRecords = new ArrayList<>();
                List<Map<String, String>> matchedRecords = new ArrayList<>();

                entitiesBySheet.get(sheet).add(queryResult.entity());

                // if we don't have any common columns, then there is no way to join the records
                if (commonColumns.size() == 0) {
                    // we need to put all records on a new sheet
                    recordsBySheet.put(sheet + "_" + queryResult.entity().getConceptAlias(), queryResult.get(false));
                    break;
                }

                // For each record, try and find an existingRecord where all commonColumns match
                // if there is only 1 existingRecord where all commonColumns match, then we can
                // do the join
                boolean newSheet = false;
                for (Map<String, String> record : queryResult.get(false)) {

                    // TODO what happens when event,sample,tissue are on same sheet, and only event & tissue are in queryResults?
                    boolean foundMatch = false;

                    for (Map<String, String> existing : existingRecords) {
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
                            matchedRecords.add(existing);
                            HashMap<String, String> merged = new HashMap<>(existing);
                            merged.putAll(record);
                            mergedRecords.add(merged);
                            foundMatch = true;
                        }
                    }

                    if (!foundMatch) {
                        // we are done here. all records need to be placed on a new sheet
                        newSheet = true;
                        break;
                    }
                }

                if (newSheet) {
                    recordsBySheet.put(sheet + "_" + queryResult.entity().getConceptAlias(), queryResult.get(false));
                } else {
                    List<Map<String, String>> records = recordsBySheet.get(sheet);
                    records.removeAll(matchedRecords);
                    records.addAll(mergedRecords);
                }

            } else {
                recordsBySheet.put(sheet, new ArrayList<>(queryResult.get(false)));
                entitiesBySheet.computeIfAbsent(sheet, k -> new ArrayList<>()).add(queryResult.entity());
            }

        }

        List<ExcelWorkbookWriter.WorkbookWriterSheet> sheets = new ArrayList<>();
        for (String sheetName : recordsBySheet.keySet()) {
            List<Map<String, String>> records = recordsBySheet.get(sheetName);

            List<String> columns = new ArrayList<>(project.getProjectConfig().attributesForSheet(sheetName)
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

            sheets.add(
                    new ExcelWorkbookWriter.WorkbookWriterSheet(sheetName, columns, records)
            );
        }
        return sheets;
    }

    private List<String> getCommonColumns(List<Entity> existingEntities, Entity entity) {
        List<String> existingColumns = existingEntities.stream()
                .flatMap(e -> e.getAttributes().stream())
                .map(Attribute::getColumn)
                .collect(Collectors.toList());

        return entity.getAttributes().stream()
                .map(Attribute::getColumn)
                .filter(existingColumns::contains)
                .collect(Collectors.toList());
    }
}
