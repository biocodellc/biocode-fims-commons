package biocode.fims.query.writers;

import biocode.fims.digester.DataType;
import biocode.fims.digester.Entity;
import biocode.fims.fimsExceptions.errorCodes.FileCode;
import biocode.fims.fimsExceptions.FimsRuntimeException;
import biocode.fims.fimsExceptions.errorCodes.QueryCode;
import biocode.fims.projectConfig.ProjectConfig;
import biocode.fims.query.QueryResult;
import biocode.fims.settings.PathManager;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.poi.hssf.usermodel.HSSFDataFormat;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author RJ Ewing
 */
public class ExcelQueryWriter implements QueryWriter {
    private final QueryResult queryResult;
    private final Entity entity;

    private SXSSFWorkbook workbook = new SXSSFWorkbook();
    private Set<String> columns;

    public ExcelQueryWriter(QueryResult queryResult) {
        this.queryResult = queryResult;
        this.entity = queryResult.entity();
    }

    @Override
    public File write() {
        File file = PathManager.createUniqueFile("output.xlsx", System.getProperty("java.io.tmpdir"));

        List<Map<String, String>> records = queryResult.get(true);

        if (records.size() == 0) {
            throw new FimsRuntimeException(QueryCode.NO_RESOURCES, 400);
        }

        this.columns = records.get(0).keySet();

        Sheet sheet = workbook.createSheet(entity.getWorksheet());

        int rowNum = 0;

        Row row = sheet.createRow(rowNum);
        writeHeaderRow(row);
        rowNum++;

        for (Map<String, String> record : records) {
            row = sheet.createRow(rowNum);

            addResourceToRow(record, row);

            rowNum++;
        }

        try (FileOutputStream fout = new FileOutputStream(file)) {
            workbook.write(fout);
        } catch (IOException e) {
            throw new FimsRuntimeException(FileCode.WRITE_ERROR, 500);
        }

        workbook.dispose();
        return file;
    }

    private void writeHeaderRow(Row row) {
        int cellNum = 0;

        for (String column : columns) {
            Cell cell = row.createCell(cellNum);

            cell.setCellValue(column);
            cellNum++;
        }
    }

    private void addResourceToRow(Map<String, String> record, Row row) {
        int cellNum = 0;

        for (String column : columns) {
            Cell cell = row.createCell(cellNum);

            cell.setCellValue(record.getOrDefault(column, ""));

            if (entity.getAttribute(column).getDatatype().equals(DataType.FLOAT)) {
                XSSFCellStyle style = (XSSFCellStyle) workbook.createCellStyle();
                style.setDataFormat(HSSFDataFormat.getBuiltinFormat("0.0"));
                cell.setCellStyle(style);
            } else if (entity.getAttribute(column).getDatatype().equals(DataType.INTEGER)) {
                XSSFCellStyle style = (XSSFCellStyle) workbook.createCellStyle();
                style.setDataFormat(HSSFDataFormat.getBuiltinFormat("0"));
                cell.setCellStyle(style);
            }

            cellNum++;
        }

    }
}
