package biocode.fims.reader.plugins;

import biocode.fims.fimsExceptions.FimsException;
import biocode.fims.fimsExceptions.FimsRuntimeException;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellReference;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * TabularDataReader for Excel-format spreadsheet files.  Both Excel 97-2003
 * format (*.xls) and Excel XML (*.xlsx) format files are supported.  The reader
 * attempts to infer if cells containing numerical values actually contain dates
 * by checking if the cell is date-formatted.  It so, the numerical value is
 * converted to a standard ISO8601 date/time string (yyyy-MM-ddTHH:mm:ss.SSSZZ).
 * This should work properly with both the Excel "1900 Date System" and the
 * "1904 Date System".  Also, the first row in each worksheet is assumed to
 * contain the column headers for the data and determines how many columns are
 * examined for all subsequent rows.
 */
public class ExcelReader implements TabularDataReader {
    // iterator for moving through the active worksheet
    protected Iterator<Row> rowIterator = null;
    protected boolean hasNext = false;

    private static Logger logger = LoggerFactory.getLogger(ExcelReader.class);

    protected Row nextRow;

    // The number of columns in the active worksheet (set by the first row).
    protected int numCols;

    // The index for the active worksheet.
    protected int currSheet;

    // The entire workbook (e.g., spreadsheet file).
    protected Workbook excelWb;

    // DataFormatter and FormulaEvaluator for dealing with cells with formulas.
    protected DataFormatter df;
    protected FormulaEvaluator fe;

    // The row # that the header values are on
    // TODO: make this adjustable in case header rows appear on another line besides the first (see bioValidator code)
    protected int numHeaderRows = 0;

    // A reference to the file that opened this reader
    protected File inputFile;

    public String getShortFormatDesc() {
        return "Microsoft Excel";
    }

    public File getInputFile() {
        return inputFile;
    }

    public String getFormatString() {
        return "EXCEL";
    }

    public String getFormatDescription() {
        return "Microsoft Excel 97-2003, 2007+";
    }

    public String[] getFileExtensions() {
        return new String[]{"xls", "xlsx"};
    }

    /**
     * See if the specified file is an Excel file.  As currently implemented,
     * this method simply tests if the file extension is "xls" or "xlsx".  A
     * better approach would be to actually encodeURIcomponent for a specific "magic number."
     * This method also tests if the file actually exists.
     *
     * @param filepath The file to encodeURIcomponent.
     *
     * @return True if the specified file exists and appears to be an Excel
     *         file, false otherwise.
     */
    public boolean testFile(String filepath) {
        // encodeURIcomponent if the file exists
        File file = new File(filepath);
        if (!file.exists())
            return false;

        int index = filepath.lastIndexOf('.');

        if (index != -1 && index != (filepath.length() - 1)) {
            // get the extension
            String ext = filepath.substring(index + 1);
            if (ext.equals("xls") || ext.equals("xlsx"))
                return true;
        }

        return false;
    }

    public boolean openFile(String filepath, String defaultSheetName, String outputFolder) {

        //fimsPrinter.out.println(filepath);
        FileInputStream is;

        try {
            is = new FileInputStream(filepath);
        } catch (FileNotFoundException e) {
            logger.warn("File not found: {}", filepath, e);
            return false;
        }

        try {
            excelWb = WorkbookFactory.create(is);

            currSheet = 0;
        } catch (InvalidFormatException e) {
            logger.warn("invalid format", e);
            return false;
        } catch (IOException e) {
            logger.warn("IOException", e);
            return false;
        }

        // Create a new DataFormatter and FormulaEvaluator to use for cells with
        // formulas.
        df = new DataFormatter();
        fe = excelWb.getCreationHelper().createFormulaEvaluator();

        // Set the input file
        inputFile = new File(filepath);

        return true;
    }

    public boolean hasNextTable() {
        if (excelWb == null)
            return false;
        else
            return (currSheet < excelWb.getNumberOfSheets());
    }

    public void setTable(String worksheet) throws FimsException {
        Sheet exsheet = excelWb.getSheet(worksheet);

        if (exsheet == null) {
            throw new FimsException("Unable to find worksheet " + worksheet);
        }
        currSheet = excelWb.getSheetIndex(worksheet) + 1;
        rowIterator = exsheet.rowIterator();
        numCols = -1;
        testNext();
    }

    public void moveToNextTable() {
        if (hasNextTable()) {
            Sheet exsheet = excelWb.getSheetAt(currSheet++);
            rowIterator = exsheet.rowIterator();
            numCols = -1;
            testNext();
        } else
            throw new NoSuchElementException();
    }

    public String getCurrentTableName() {
        return excelWb.getSheetName(currSheet - 1);
    }

    public boolean tableHasNextRow() {
        if (rowIterator == null)
            return false;
        else
            return hasNext;
    }

    /**
     * Internal method to see if there is another line with data remaining in
     * the current table.  Any completely blank lines will be skipped.  This
     * method is necessary because the POI row iterator does not always reliably
     * end with the last data-containing row.
     */
    protected void testNext() {
        int lastcellnum = 0;
        while (rowIterator.hasNext() && lastcellnum < 1) {
            nextRow = rowIterator.next();
            lastcellnum = nextRow.getLastCellNum();
        }

        hasNext = lastcellnum > 0;
    }

    /**
     * Get the column names associated with a particular sheet
     *
     * @return List of Column names
     */
    public java.util.List<String> getColNames() {
        Sheet wsh = excelWb.getSheet(getCurrentTableName());

        java.util.List<String> listColumnNames = new ArrayList<String>();
        Iterator<Row> rows = wsh.rowIterator();
        int count = 0;
        while (rows.hasNext()) {
            if (count == numHeaderRows) {
                break;
            }
            rows.next();
            count++;
        }

        //XSSFRow row = (XSSFRow) rows.next();
        Row row = (Row) rows.next();

        Iterator<Cell> cells = row.cellIterator();
        while (cells.hasNext()) {
            //XSSFCell cell = (XSSFCell) cells.next();
            Cell cell = (Cell) cells.next();
            if (cell.toString().trim() != "" && cell.toString() != null) {
                listColumnNames.add(cell.toString());
            }
        }

        return listColumnNames;
    }

    /**
     * get a particular sheet
     *
     * @return
     */
    public Sheet getSheet() {
        return excelWb.getSheet(getCurrentTableName());
    }

    /**
     * Secure way to count number of rows in spreadsheet --- this method finds the first blank row and then returns the
     * count--- this
     * means there can be no blank rows.
     *
     * @return a count of number of rows
     */
    public Integer getNumRows() {

        Sheet wsh = excelWb.getSheet(getCurrentTableName());
        Integer numRows;

        Iterator it = wsh.rowIterator();
        int count = 0;
        while (it.hasNext()) {
            Row row = (Row) it.next();
            Iterator cellit = row.cellIterator();
            String rowContents = "";
            while (cellit.hasNext()) {
                Cell cell = (Cell) cellit.next();
                if (cell.getCellType() == Cell.CELL_TYPE_STRING || cell.getCellType() == Cell.CELL_TYPE_NUMERIC) {
                    rowContents += cell.toString();
                }
            }
            if (rowContents.equals("")) {
                // The count to return should be minus 1 to account for title
                numRows = count - 1 - numHeaderRows;
                return numRows;
            }
            count++;
        }
        // The count to return should be minus 1 to account for title
        numRows = count - 1 - numHeaderRows;

        return numRows;
    }


    /**
     * Get the value of a particular row for a particular column
     *
     * @param column
     * @param row
     *
     * @return value of this cell
     */
    public String getStringValue(String column, int row) {
        String strValue = getStringValue(getColumnPosition(column), row);
        return strValue;
    }

    /**
     * Returns string values for all cells regardless of whether they are cast as numeric or
     * String.  Does not handle boolean cell types currently.
     *
     * @param col
     * @param row
     *
     * @return
     */
    public String getStringValue(int col, int row) {
        Sheet wsh = excelWb.getSheet(getCurrentTableName());
        row = row + this.numHeaderRows;

        Row hrow = wsh.getRow(row);
        Cell cell = hrow.getCell(col);
        try {
            if (cell.getCellType() == Cell.CELL_TYPE_BLANK) {
                return null;
            } else if (cell.getCellType() == Cell.CELL_TYPE_STRING) {
                if (cell.getStringCellValue().trim().equals("")) {
                    return null;
                } else {
                    return cell.toString();
                }
                // Handle Numeric Cell values--- this is a bit strange since we have no way of
                // knowing if the numeric cell value is integer or double.  Thus, "5.0" gets interpreted
                // as "5"... this is probably preferable to "5" getting displayed as "5.0", which is the
                // default HSSFCell value behaviour
            } else if (cell.getCellType() == Cell.CELL_TYPE_NUMERIC) {
                if (new Double(cell.getNumericCellValue()).toString().trim().equals("")) {
                    return null;
                } else {
                    String value = Double.toString(cell.getNumericCellValue()); //toString();
                    if (value.indexOf(".0") == value.length() - 2) {
                        Double D = (Double.parseDouble(value));
                        Integer I = D.intValue();
                        return I.toString();
                    } else {
                        return value;
                    }
                }
            } else {
                return null;
            }
        } catch (NullPointerException e) {
            logger.warn("NullPointerException", e);
            return null;
            // case where this may be a numeric cell lets still try and return a string
        } catch (IllegalStateException e) {
            logger.warn("IllegalStateException", e);
            return null;
        }
    }

    /**
     * Get the value of a cell as a double
     *
     * @param column
     * @param row
     *
     * @return double value in this cell
     */
    public Double getDoubleValue(String column, int row) {
        Double dblValue = null;
        String strValue = getStringValue(column, row);

        if (strValue != null) {
            dblValue = Double.parseDouble(strValue);
        }
        return dblValue;
    }

    /**
     * find the position of a column as an integer
     *
     * @param colName
     *
     * @return integer of the column position
     */
    public Integer getColumnPosition(String colName) {
        java.util.List<String> listColumns = this.getColNames();
        for (int i = 0; i < listColumns.size(); i++) {
            //fimsPrinter.out.println("\tarray val = " + this.getColNames().toArray()[i].toString());
            if (this.getColNames().toArray()[i].toString().equals(colName)) {
                return i;
            }
        }
        // if not found then throw exception
        return null;
    }

    public String[] tableGetNextRow() {
        if (!tableHasNextRow())
            throw new NoSuchElementException();

        Row row = nextRow;
        Cell cell;

        // If this is the first row in the sheet, use it to determine how many
        // columns this sheet has.  This is necessary to make sure that all rows
        // have the same number of cells for SQLite.
        if (numCols < 0)
            numCols = row.getLastCellNum();

        String[] ret = new String[numCols];

        // Unfortunately, we can't use a cell iterator here because, as
        // currently implemented in POI, iterating over cells in a row will
        // silently skip blank cells.
        for (int cnt = 0; cnt < numCols; cnt++) {
            cell = row.getCell(cnt, Row.CREATE_NULL_AS_BLANK);
            // inspect the data type of this cell and act accordingly
            switch (cell.getCellType()) {
                case Cell.CELL_TYPE_STRING:
                    // Always trim spaces from before and after data, makes data processing easier later
                    ret[cnt] = cell.getStringCellValue().trim();
                    break;
                case Cell.CELL_TYPE_NUMERIC:
                    // There is no date data type in Excel, so we have to check
                    // if this cell contains a date-formatted value.
                    //if (HSSFDateUtil.isCellDateFormatted(cell)) {
                    if (DateUtil.isCellDateFormatted(cell)) {
                        // Convert the value to a Java date object, then to
                        // ISO 8601 format using Joda-Time.
                        DateTime date;
                        date = new DateTime(cell.getDateCellValue());
                        ret[cnt] = date.toString();
                    } else {
                        // TODO: Fix this rendering.  They both are bad!!!

                        // This one works for BCID buts messes up latitude / longitude values
                        // Set celltype back to String here since this is a more reliable rendering of the input data,
                        // as Excel actually sees it.  The Numeric type adds additional ".0"'s on the end...
                        //cell.setCellType(Cell.CELL_TYPE_STRING);
                        //ret[cnt] = cell.getStringCellValue();


                        // This one works for latitude/longitude values rendering appropriately
                        // but adds a .0 to BCID in the output-- BAD
                        //ret[cnt] = cell.toString();

                        // This option, using Decimal Format seems to work for everything
                        cell.setCellType(Cell.CELL_TYPE_STRING);
                        DecimalFormat pattern = new DecimalFormat("#,#,#,#,#,#,#,#,#,#");
                        Number n = null;
                        try {
                            n = pattern.parse(cell.getStringCellValue());
                        } catch (ParseException e) {
                            e.printStackTrace();
                        }
                        ret[cnt] = n.toString();
                    }
                    break;
                case Cell.CELL_TYPE_BOOLEAN:
                    if (cell.getBooleanCellValue())
                        ret[cnt] = "true";
                    else
                        ret[cnt] = "false";
                    break;
                case Cell.CELL_TYPE_FORMULA:
                    // Use the FormulaEvaluator to determine the result of the
                    // cell's formula, then convert the result to a String.
                    // The following was throwing an error...
                    try {
                        ret[cnt] = df.formatCellValue(cell, fe);
                    } catch (Exception e) {
                        //TODO should we be catching Exception?
                        int rowNum = cell.getRowIndex() + 1;
                        throw new FimsRuntimeException("There was an issue processing a formula on this sheet.\n" +
                                "\tWhile standard formulas are allowed, formulas with references to external sheets cannot be read!\n" +
                                "\tCell = " + CellReference.convertNumToColString(cnt) + rowNum + "\n" +
                                "\tUnreadable Formula = " + cell + "\n" +
                                "\t(There may be additional formulas you may need to fix)", "Exception", 400, e
                        );
                    }
                    //ret[cnt] = df.formatCellValue(cell);
                    break;
                default:
                    ret[cnt] = "";
            }
        }
        //System.out.println(ret);
        // Determine if another row is available after this one.
        testNext();

        return ret;
    }

    public void closeFile() {
    }
}
