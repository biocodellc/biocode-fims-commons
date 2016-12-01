package biocode.fims.query;

import biocode.fims.digester.DataType;
import biocode.fims.fimsExceptions.FileCode;
import biocode.fims.fimsExceptions.FimsRuntimeException;
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

import javax.xml.crypto.Data;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;

/**
 * Class to write Json to an excel file
 *
 * @author RJ Ewing
 */
public class ExcelJsonWriter implements JsonWriter {
    private final ArrayNode resources;
    private final String outputDirectory;

    private final String sheetName;
    private final List<JsonFieldTransform> columns;
    private boolean writeHeader = true;
    private SXSSFWorkbook workbook = new SXSSFWorkbook();

    /**
     * @param resources       {@link ArrayNode} of {@link ObjectNode}'s to be written to the excel file.
     *                        Each {@link ObjectNode#fields()} may only contain {@link ObjectNode} or basic java data type (String, Integer, int, etc...)
     * @param columns
     * @param sheetName
     * @param outputDirectory
     */
    public ExcelJsonWriter(ArrayNode resources, List<JsonFieldTransform> columns, String sheetName,
                           String outputDirectory) {
        this.resources = resources;
        this.outputDirectory = outputDirectory;
        this.columns = columns;

        this.sheetName = sheetName;
    }

    /**
     * @param resources       {@link ArrayNode} of {@link ObjectNode}'s to be written to the excel file.
     *                        Each {@link ObjectNode#fields()} may only contain {@link ObjectNode} or basic java data type (String, Integer, int, etc...)
     * @param columns
     * @param sheetName
     * @param outputDirectory
     * @param writeHeader
     */
    public ExcelJsonWriter(ArrayNode resources, List<JsonFieldTransform> columns, String sheetName,
                           String outputDirectory, boolean writeHeader) {
        this(resources, columns, sheetName, outputDirectory);
        this.writeHeader = writeHeader;
    }

    @Override
    public File write() {
        File file = PathManager.createUniqueFile("output.xlsx", outputDirectory);

        Sheet sheet = workbook.createSheet(sheetName);

        int rowNum = 0;

        if (writeHeader) {
            Row row = sheet.createRow(rowNum);
            writeHeaderRow(row);
            rowNum++;
        }

        for (JsonNode resource : resources) {
            Row row = sheet.createRow(rowNum);

            // currently assuming that all resources are objects
            addResourceToRow((ObjectNode) resource, row);

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

        for (JsonFieldTransform column : columns) {
            Cell cell = row.createCell(cellNum);

            cell.setCellValue(column.getFieldName());
            cellNum++;
        }
    }

    private void addResourceToRow(ObjectNode resource, Row row) {
        int cellNum = 0;

        for (JsonFieldTransform column : columns) {
            Cell cell = row.createCell(cellNum);

            cell.setCellValue(resource.at(column.getPath()).textValue());

            if (column.getDataType().equals(DataType.FLOAT)) {
                XSSFCellStyle style = (XSSFCellStyle) workbook.createCellStyle();
                style.setDataFormat(HSSFDataFormat.getBuiltinFormat("0.0"));
                cell.setCellStyle(style);
            } else if (column.getDataType().equals(DataType.INTEGER)) {
                XSSFCellStyle style = (XSSFCellStyle) workbook.createCellStyle();
                style.setDataFormat(HSSFDataFormat.getBuiltinFormat("0"));
                cell.setCellStyle(style);
            }

            cellNum++;
        }

    }
}
