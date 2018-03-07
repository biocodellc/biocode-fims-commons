package biocode.fims.query.writers;

import biocode.fims.digester.Attribute;
import biocode.fims.digester.DataType;
import biocode.fims.digester.Entity;
import biocode.fims.fimsExceptions.errorCodes.FileCode;
import biocode.fims.fimsExceptions.FimsRuntimeException;
import biocode.fims.fimsExceptions.errorCodes.QueryCode;
import biocode.fims.query.QueryResult;
import biocode.fims.query.QueryResults;
import biocode.fims.settings.PathManager;
import org.apache.poi.hssf.usermodel.HSSFDataFormat;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author RJ Ewing
 */
public class ExcelQueryWriter implements QueryWriter {
    private final QueryResults queryResults;

    private SXSSFWorkbook workbook = new SXSSFWorkbook();

    public ExcelQueryWriter(QueryResults queryResults) {
        this.queryResults = queryResults;
    }

    @Override
    public File write() {
        File file = PathManager.createUniqueFile("output.xlsx", System.getProperty("java.io.tmpdir"));

        if (queryResults.isEmpty()) {
            throw new FimsRuntimeException(QueryCode.NO_RESOURCES, 400);
        }

        Map<String, List<Map<String, String>>> recordsBySheet = recordsBySheet();

        recordsBySheet.forEach((sheetName, records) -> {
            List<String> columns = new ArrayList(records.get(0).keySet());
            Sheet sheet = workbook.createSheet(sheetName);

            int rowNum = 0;

            Row row = sheet.createRow(rowNum);
            writeHeaderRow(columns, row);
            rowNum++;

            for (Map<String, String> record : records) {
                row = sheet.createRow(rowNum);

                addResourceToRow(sheetName, columns, record, row);
                rowNum++;
            }
        });

        try (FileOutputStream fout = new FileOutputStream(file)) {
            workbook.write(fout);
        } catch (IOException e) {
            throw new FimsRuntimeException(FileCode.WRITE_ERROR, 500);
        }

        workbook.dispose();
        return file;
    }

    private Map<String, List<Map<String, String>>> recordsBySheet() {
        Map<String, List<Map<String, String>>> recordsBySheet = new HashMap<>();
        Map<String, List<Entity>> entitiesBySheet = new HashMap<>();

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

                entitiesBySheet.get(sheet).add(queryResult.entity());

                // if we don't have any common columns, then there is no way to join the records
                if (commonColumns.size() == 0) {
                    // we need to put all records on a new sheet
                    recordsBySheet.put(sheet + "_" + queryResult.entity().getConceptAlias(), queryResult.get(true));
                    break;
                }

                // For each record, try and find an existingRecord where all commonColumns match
                // if there is only 1 existingRecord where all commonColumns match, then we can
                // do the join
                boolean newSheet = false;
                for (Map<String, String> record : queryResult.get(true)) {

                    boolean matches = true;
                    Map<String, String> match = null;

                    for (Map<String, String> existing : existingRecords) {

                        // check if we can join record and existing
                        for (String col : commonColumns) {
                            if (!Objects.equals(existing.get(col), record.get(col))) {
                                matches = false;
                                break;
                            }
                        }

                        if (matches && match != null) {
                            // this means that more then 1 existingRecord matches, so we can't do the join
                            matches = false;
                            break;
                        } else if (matches) {
                            match = existing;
                        }
                    }

                    if (matches) {
                        // merge w/ existing record by adding all properties
                        match.putAll(record);
                        mergedRecords.add(match);
                    } else {
                        // we are done here. all records need to be placed on a new sheet
                        newSheet = true;
                        break;
                    }
                }

                if (newSheet) {
                    recordsBySheet.put(sheet + "_" + queryResult.entity().getConceptAlias(), queryResult.get(true));
                } else {
                    recordsBySheet.put(sheet, mergedRecords);
                }

            } else {
                recordsBySheet.put(sheet, new ArrayList(queryResult.records()));
                entitiesBySheet.computeIfAbsent(sheet, k -> new ArrayList<>()).add(queryResult.entity());
            }

        }
        return recordsBySheet;
    }

    private List<String> getCommonColumns(List<Entity> existingEntities, Entity entity) {
        List<String> existingColumns = existingEntities.stream()
                .flatMap(e -> e.getAttributes().stream())
                .map(Attribute::getColumn)
                .collect(Collectors.toList());

        return entity.getAttributes().stream()
                .map(a -> a.getColumn())
                .filter(c -> existingColumns.contains(c))
                .collect(Collectors.toList());
    }

    private void writeHeaderRow(List<String> columns, Row row) {
        int cellNum = 0;

        for (String column : columns) {
            Cell cell = row.createCell(cellNum);

            cell.setCellValue(column);
            cellNum++;
        }
    }

    private void addResourceToRow(String sheetName, List<String> columns, Map<String, String> record, Row row) {
        int cellNum = 0;

        for (String column : columns) {
            Cell cell = row.createCell(cellNum);

            cell.setCellValue(record.getOrDefault(column, ""));

            DataType dataType = findDataType(sheetName, column);
            if (dataType.equals(DataType.FLOAT)) {
                XSSFCellStyle style = (XSSFCellStyle) workbook.createCellStyle();
                style.setDataFormat(HSSFDataFormat.getBuiltinFormat("0.0"));
                cell.setCellStyle(style);
            } else if (dataType.equals(DataType.INTEGER)) {
                XSSFCellStyle style = (XSSFCellStyle) workbook.createCellStyle();
                style.setDataFormat(HSSFDataFormat.getBuiltinFormat("0"));
                cell.setCellStyle(style);
            }

            cellNum++;
        }
    }

    private DataType findDataType(String sheetName, String column) {
        return queryResults.entities().stream()
                .filter(e -> sheetName.equals(e.getWorksheet()) && e.getAttribute(column) != null)
                .findFirst()
                .map(e -> e.getAttribute(column).getDatatype())
                .orElse(DataType.STRING);
    }
}
