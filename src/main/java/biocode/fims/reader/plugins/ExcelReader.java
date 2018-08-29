package biocode.fims.reader.plugins;

import biocode.fims.config.models.Entity;
import biocode.fims.config.project.ProjectConfig;
import biocode.fims.fimsExceptions.FimsRuntimeException;
import biocode.fims.fimsExceptions.errorCodes.DataReaderCode;
import biocode.fims.records.Record;
import biocode.fims.records.RecordMetadata;
import biocode.fims.reader.DataReader;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellReference;
import org.joda.time.LocalDate;
import org.joda.time.LocalDateTime;
import org.joda.time.LocalTime;

import java.io.File;
import java.io.IOException;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.util.*;

/**
 * DataReader implementation for Excel-format workbook files.  Both Excel 97-2003
 * format (*.xls) and Excel XML (*.xlsx) format files are supported.  The reader
 * attempts to infer if cells containing numerical values actually contain dates
 * by checking if the cell is date-formatted.  It so, the numerical value is
 * converted to a standard ISO8601 date/time string (yyyy-MM-ddTHH:mm:ss.SSSZZ).
 * This should work properly with both the Excel "1900 Date System" and the
 * "1904 Date System".  Also, the first row in each worksheet is assumed to
 * contain the column headers for the data and determines how many columns are
 * examined for all subsequent rows.
 *
 * The Reader does not use any RecordMetadata
 *
 */
public class ExcelReader extends AbstractTabularDataReader {
    public static final List<String> EXTS = Arrays.asList("xlsx", "xls");

    protected DataFormatter dataFormatter;
    protected FormulaEvaluator formulaEvaluator;

    protected Workbook excelWb;

    private Sheet currSheet;
    protected Iterator<Row> rowIterator = null;
    protected int numCols;

    public ExcelReader() {
    }

    public ExcelReader(File file, ProjectConfig projectConfig, RecordMetadata recordMetadata) {
        super(file, projectConfig, recordMetadata);

        this.entityRecords = new HashMap<>();
        this.dataFormatter = new DataFormatter();
    }

    @Override
    public boolean handlesExtension(String ext) {
        return EXTS.contains(ext.toLowerCase());
    }

    @Override
    public DataReader newInstance(File file, ProjectConfig projectConfig, RecordMetadata recordMetadata) {
        return new ExcelReader(file, projectConfig, recordMetadata);
    }

    @Override
    void init() {
        try {
            excelWb = WorkbookFactory.create(file);
            formulaEvaluator = excelWb.getCreationHelper().createFormulaEvaluator();
        } catch (InvalidFormatException | IOException e) {
            throw new FimsRuntimeException(DataReaderCode.READ_ERROR, 500);
        }
    }

    @Override
    void instantiateRecords() {

        for (Sheet sheet : excelWb) {
            currSheet = sheet;
            colNames = null;
            numCols = -1;
            sheetEntities = config.entitiesForSheet(currSheet.getSheetName());

            if (sheetEntities.size() > 0) {
                instantiateRecordsForCurrentSheet();
            }
        }

        boolean hasRecords = false;
        for (Map.Entry<Entity, List<Record>> entry: entityRecords.entrySet()) {
            if (entry.getValue().size() > 0) {
                hasRecords = true;
                break;
            }
        }

        if (!hasRecords) {
            throw new FimsRuntimeException(DataReaderCode.NO_DATA, 400, currSheet.getSheetName());
        }
    }

    private void instantiateRecordsForCurrentSheet() {

        rowIterator = currSheet.rowIterator();

        if (rowIterator.hasNext()) {
            setColumnNames();

            while (rowIterator.hasNext()) {
                instantiateRecordsFromRow(nextRow());
            }
        }

    }

    private void setColumnNames() {
        // Get the first row to populate Column Names
        colNames = nextRow();

        Set<String> colSet = new HashSet<>();

        for (String col : colNames) {
            if (!colSet.add(col)) {
                throw new FimsRuntimeException(DataReaderCode.DUPLICATE_COLUMNS, 400, currSheet.getSheetName() + " worksheet", col);
            }
        }
    }

    private LinkedList<String> nextRow() {
        LinkedList<String> retRow = new LinkedList<>();

        if (!rowIterator.hasNext()) {
            return retRow;
        }

        Cell cell;
        Row row = rowIterator.next();

        // If this is the first row in the sheet, use it to determine how many
        // columns this sheet has.
        if (numCols < 0)
            numCols = getNumCols(row);


        // Unfortunately, we can't use a cell iterator here because, as
        // currently implemented in POI, iterating over cells in a row will
        // silently skip blank cells.
        for (int colNum = 0; colNum < numCols; colNum++) {
            cell = row.getCell(colNum, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK);

            switch (cell.getCellTypeEnum()) {
                case STRING:
                    retRow.add(cell.getStringCellValue().trim());
                    break;
                case NUMERIC:
                    // There is no date data type in Excel, so we have to check
                    // if this cell contains a date-formatted value.
                    if (DateUtil.isCellDateFormatted(cell)) {
                        // Convert the value to ISO 8601 format using Joda-Time.
                        // Since excel stores date, time, and datetime as a numeric cell, we need to do our best
                        // to determine what the value should be.
                        String value;
                        LocalDateTime date = new LocalDateTime(cell.getDateCellValue());
                        if (date.toLocalDate().equals(new LocalDate("1899-12-31")) && !date.toLocalTime().equals(new LocalTime("00:00:00.000"))) {
                            // for a time cell, getDateCellValue will return the datetime object with the date
                            // "1899-12-31" and the correct time. Therefore if we encounter this we assume this is a time cell
                            value = date.toLocalTime().toString();
                        } else if (date.toLocalTime().equals(new LocalTime("00:00:00.000"))) {
                            value = date.toLocalDate().toString();
                        } else {
                            value = date.toString();
                        }

                        retRow.add(value);
                    } else {
                        cell.setCellType(CellType.STRING);
                        DecimalFormat pattern = new DecimalFormat("#,#,#,#,#,#,#,#,#,#");
                        try {
                            Number n = pattern.parse(cell.getStringCellValue());
                            retRow.add(String.valueOf(n));
                        } catch (ParseException e) {
                            retRow.add(cell.getStringCellValue());
                        }
                    }
                    break;
                case BOOLEAN:
                    if (cell.getBooleanCellValue())
                        retRow.add("true");
                    else
                        retRow.add("false");
                    break;
                case FORMULA:
                    try {
                        retRow.add(dataFormatter.formatCellValue(cell, formulaEvaluator));
                    } catch (Exception e) {
                        int rowNum = cell.getRowIndex() + 1;
                        throw new FimsRuntimeException("There was an issue processing a formula on this sheet.\n" +
                                "\tWhile standard formulas are allowed, formulas with references to external sheets cannot be read!\n" +
                                "\tCell = " + CellReference.convertNumToColString(colNum) + rowNum + "\n" +
                                "\tUnreadable Formula = " + cell + "\n" +
                                "\t(There may be additional formulas you may need to fix)", "Exception", 400, e
                        );
                    }
                    break;
                default:
                    retRow.add("");
            }
        }

        return retRow;
    }

    /**
     * This method returns the number of cells in the given {@link Row}. After 10 blank columns, this function assumes
     * that there are no more columns in the {@link Row}. If there is a possibility that there are more then 10 blank
     * cells in a {@link Row}, DO NOT USE THIS METHOD. Try {@link Row}'s .getLastCellNum
     *
     * @param headerRow
     * @return The number of columns in the {@link Row}
     */
    private int getNumCols(Row headerRow) {
        int consecutiveBlankCells = 0;
        int cellCount = 0;
        Cell cell;
        for (int col = 0; col < headerRow.getLastCellNum(); col++) {
            cell = headerRow.getCell(col, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
            if (cell == null) {
                consecutiveBlankCells++;
            } else {
                consecutiveBlankCells = 0;
            }

            cellCount++;

            if (consecutiveBlankCells == 10) {
                cellCount = cellCount - 10;
                break;
            }
        }
        return cellCount;
    }
}
