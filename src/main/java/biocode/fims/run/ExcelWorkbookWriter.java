package biocode.fims.run;

import biocode.fims.fimsExceptions.FimsRuntimeException;
import biocode.fims.fimsExceptions.errorCodes.FileCode;
import biocode.fims.models.Project;
import biocode.fims.models.User;
import biocode.fims.config.models.Attribute;
import biocode.fims.config.models.Field;
import biocode.fims.query.writers.WriterWorksheet;
import biocode.fims.utils.FileUtils;
import biocode.fims.validation.rules.ControlledVocabularyRule;
import biocode.fims.validation.rules.RuleLevel;
import org.apache.poi.hssf.util.CellReference;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddressList;
import org.apache.poi.xssf.usermodel.*;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;


/**
 * Generates excel template workbooks. Includes worksheets and optional data, instructions, and basic data validation
 */
public class ExcelWorkbookWriter {
    private final static String INSTRUCTIONS_SHEET_NAME = "Instructions";
    private final static String DATA_FIELDS_SHEET_NAME = "Fields";
    private final static String LISTS_SHEET_NAME = "Lists";
    private final static String WARNING_MSG = "The value you entered is not from the recommended list. This will create a warning upon validation.";
    private final static String ERROR_MSG = "The value you entered is not from the recommended list. This will create an error upon validation.";

    protected final Project project;
    private final int naan;
    private final User user;
    private final XSSFWorkbook workbook;

    private CellStyle headingStyle;
    private CellStyle requiredStyle;
    private CellStyle wrapStyle;
    private CellStyle regularStyle;

    public ExcelWorkbookWriter(Project project, int naan) {
        this(project, naan, null);
    }

    public ExcelWorkbookWriter(Project project, int naan, User user) {
        this.project = project;
        this.naan = naan;
        this.user = user;
        this.workbook = new XSSFWorkbook();
        initWorkbookStyles();
    }

    public File write(List<WriterWorksheet> sheets) {
        // Create each of the sheets
        createInstructions(
                sheets.stream()
                        .map(s -> s.sheetName)
                        .collect(Collectors.toList())
        );

        for (WriterWorksheet sheet : sheets) {
            createSheet(sheet);
            createDataFields(sheet.sheetName, sheet.columns);
        }

        for (int i = 0; i < sheets.size(); i++) {
            // re-order worksheets towards the beginning
            workbook.setSheetOrder(sheets.get(i).sheetName, i + 1); // +1 b/c instructions sheet is first
        }

        createListsSheetAndValidations(sheets);

        // Create the output Filename and Write Excel File
        String filename = project.getProjectTitle() + ".xlsx";
        File file = FileUtils.createUniqueFile(filename, System.getProperty("java.io.tmpdir"));
        try {
            FileOutputStream out = new FileOutputStream(file);
            workbook.write(out);
            out.close();
        } catch (IOException e) {
            throw new FimsRuntimeException(FileCode.WRITE_ERROR, 500);
        }

        return file;
    }

    /**
     * Create an instructions sheet
     */
    private void createInstructions(List<String> worksheets) {
        // Create the Instructions Sheet, which is always first
        XSSFSheet instructionsSheet = workbook.createSheet(INSTRUCTIONS_SHEET_NAME);
        Row row;
        Cell cell;
        Integer rowIndex = 0;

        // Center align & bold for title
        CellStyle titleStyle = workbook.createCellStyle();
        Font bold = workbook.createFont();
        bold.setFontHeightInPoints((short) 14);

        bold.setBold(true);
        titleStyle.setFont(bold);
        titleStyle.setAlignment(HorizontalAlignment.CENTER);

        // Make a big first column
        instructionsSheet.setColumnWidth(0, 160 * 256);

        // Hide the projectId in the first row
        row = instructionsSheet.createRow(rowIndex);
        rowIndex++;
        rowIndex++;

        // Hide NAAN in first row, first column
        cell = row.createCell(0);
        cell.setCellValue("~naan=" + naan + "~");

        // Hide Project_id in first row, second column
        cell = row.createCell(1);
        cell.setCellValue("~project_id=" + project.getProjectId() + "~");

        row.setZeroHeight(true);

        // The name of this project as specified by the sheet
        row = instructionsSheet.createRow(rowIndex);
        rowIndex++;
        cell = row.createCell(0);
        cell.setCellStyle(titleStyle);
        cell.setCellValue(project.getProjectTitle());


        // Print todays date with user name
        row = instructionsSheet.createRow(rowIndex);
        rowIndex++;
        cell = row.createCell(0);
        cell.setCellStyle(titleStyle);
        DateFormat dateFormat = new SimpleDateFormat("MMMMM dd, yyyy");
        Calendar cal = Calendar.getInstance();
        String dateAndUser = "Template generated ";
        if (user != null) {
            dateAndUser += "by '" + user.getUsername() + "' ";
        }
        dateAndUser += "on " + dateFormat.format(cal.getTime());
        cell.setCellValue(dateAndUser);

        row = instructionsSheet.createRow(rowIndex);
        rowIndex++;
        cell = row.createCell(0);
        cell.setCellStyle(titleStyle);
        cell.setCellValue("Person(s) responsible for data entry [                       ]");

        // Insert additional row before next content
        rowIndex++;

        // worksheet instructions

        row = instructionsSheet.createRow(rowIndex);
        rowIndex++;
        cell = row.createCell(0);
        cell.setCellStyle(headingStyle);
        cell.setCellValue(
                worksheets.stream()
                        .collect(Collectors.joining(", "))
                        .concat(worksheets.size() > 1 ? " Tabs" : " Tab")
        );

        row = instructionsSheet.createRow(rowIndex);
        rowIndex++;
        rowIndex++;
        cell = row.createCell(0);
        cell.setCellStyle(wrapStyle);
        String joinedSheets = worksheets.stream()
                .collect(Collectors.joining("\", \""))
                .concat(worksheets.size() > 1 ? " tabs" : " tab");
        cell.setCellValue("Please fill out each field in the " + joinedSheets + " as completely as possible. " +
                "Fields in red are required (data cannot be uploaded to the database without these fields). " +
                "Required and recommended fields are usually placed towards the beginning of the template. " +
                "Some fields have a controlled vocabulary associated with them in the \"" + LISTS_SHEET_NAME + "\" tab " +
                "and are provided as data validation in the provided cells" +
                "If you have more than one entry to a field (i.e. a list of publications), " +
                "please delimit your list with pipes (|).  Also please make sure that there are no newline " +
                "characters (=carriage returns) in any of your metadata. Fields in the " + joinedSheets + " may be re-arranged " +
                "in any order so long as you don't change the field names.");

        // data Fields sheet
        row = instructionsSheet.createRow(rowIndex);
        rowIndex++;
        cell = row.createCell(0);
        cell.setCellStyle(headingStyle);
        cell.setCellValue(
                worksheets.stream()
                        .map(s -> s + "_" + DATA_FIELDS_SHEET_NAME)
                        .collect(Collectors.joining(", "))
                        .concat(worksheets.size() > 1 ? " Tabs" : " Tab")
        );

        row = instructionsSheet.createRow(rowIndex);
        rowIndex++;
        rowIndex++;
        cell = row.createCell(0);
        cell.setCellStyle(wrapStyle);
        cell.setCellValue("This tab contains column names, associated URIs and definitions for each column.");

        //Lists Tab
        row = instructionsSheet.createRow(rowIndex);
        rowIndex++;
        cell = row.createCell(0);
        cell.setCellStyle(headingStyle);
        cell.setCellValue(LISTS_SHEET_NAME + " Tab");

        row = instructionsSheet.createRow(rowIndex);
        cell = row.createCell(0);
        cell.setCellStyle(wrapStyle);
        cell.setCellValue("This tab contains controlled vocabulary lists for certain fields.  DO NOT EDIT this sheet!");

    }

    /**
     * Create a worksheet
     */
    private void createSheet(WriterWorksheet sheet) {
        // Create the Default Sheet sheet
        XSSFSheet worksheet = workbook.createSheet(sheet.sheetName);

        //Create the header row
        int rowNum = 0;
        XSSFRow row = worksheet.createRow(rowNum);
        writeHeaderRow(sheet, row);

        rowNum++;

        for (Map<String, String> record : sheet.data) {
            row = worksheet.createRow(rowNum);

            addDataToRow(sheet, record, row);
            rowNum++;
        }

        // Auto-size the columns so we can see them all to begin with
        for (int i = 0; i <= sheet.columns.size(); i++) {
            worksheet.autoSizeColumn(i);
        }
    }

    private void writeHeaderRow(WriterWorksheet sheet, Row row) {
        // First find all the required columns so we can look them up
        Set<String> requiredColumns = this.project.getProjectConfig().getRequiredColumns(sheet.sheetName, RuleLevel.ERROR);

        int columnNum = 0;
        for (String col : sheet.columns) {
            Cell cell = row.createCell(columnNum++);
            cell.setCellValue(col);
            cell.setCellStyle(headingStyle);

            // Make required columns red
            if (requiredColumns.contains(col)) {
                cell.setCellStyle(requiredStyle);
            }
        }
    }


    private void addDataToRow(WriterWorksheet sheet, Map<String, String> record, Row row) {
        int cellNum = 0;

        for (String column : sheet.columns) {
            String val = record.get(column);

            if (val != null && !val.equals("")) {
                Cell cell = row.createCell(cellNum);
                cell.setCellValue(val);
            }

            cellNum++;
        }
    }

    /**
     * Create the DataFields sheet
     */
    private void createDataFields(String sheetName, List<String> columns) {
        XSSFSheet dataFieldsSheet = workbook.createSheet(sheetName + "_" + DATA_FIELDS_SHEET_NAME);

        // First find all the required columns so we can look them up
        Set<String> requiredColumns = this.project.getProjectConfig().getRequiredColumns(sheetName, RuleLevel.ERROR);

        int rowNum = 0;
        Row row = dataFieldsSheet.createRow(rowNum++);

        // HEADER ROWS
        Cell cell = row.createCell(DATA_FIELDS_COLUMNS.NAME);
        cell.setCellStyle(headingStyle);
        cell.setCellValue("ColumnName");

        cell = row.createCell(DATA_FIELDS_COLUMNS.DEFINITION);
        cell.setCellStyle(headingStyle);
        cell.setCellValue("Definition");

        cell = row.createCell(DATA_FIELDS_COLUMNS.CONTROLLED_VOCABULARY);
        cell.setCellStyle(headingStyle);
        cell.setCellValue("Controlled Vocabulary (see Lists)");

        cell = row.createCell(DATA_FIELDS_COLUMNS.DATA_FORMAT);
        cell.setCellStyle(headingStyle);
        cell.setCellValue("Data Format");

        List<ControlledVocabularyRule> vocabularyRules = vocabRules(sheetName);

        for (Attribute a : project.getProjectConfig().attributesForSheet(sheetName)) {

            if (columns.contains(a.getColumn())) {
                row = dataFieldsSheet.createRow(rowNum++);

                // Column Name
                Cell nameCell = row.createCell(DATA_FIELDS_COLUMNS.NAME);
                nameCell.setCellValue(a.getColumn());
                CellStyle nameStyle = requiredColumns.contains(a.getColumn())
                        ? requiredStyle
                        : headingStyle;
                nameStyle.setVerticalAlignment(VerticalAlignment.TOP);
                nameCell.setCellStyle(nameStyle);

                // Definition
                if (a.getDefinition() != null) {
                    Cell defCell = row.createCell(DATA_FIELDS_COLUMNS.DEFINITION);
                    defCell.setCellValue(a.getDefinition());
                    defCell.setCellStyle(wrapStyle);
                }

                // Controlled Vocabulary
                for (ControlledVocabularyRule r : vocabularyRules) {
                    if (r.column().equals(a.getColumn())) {
                        Cell controlledVocabCell = row.createCell(DATA_FIELDS_COLUMNS.CONTROLLED_VOCABULARY);
                        controlledVocabCell.setCellValue(r.listName());
                        controlledVocabCell.setCellStyle(wrapStyle);
                        break;
                    }
                }

                // Data Format
                if (a.getDataFormat() != null) {
                    Cell formatCell = row.createCell(DATA_FIELDS_COLUMNS.DATA_FORMAT);
                    formatCell.setCellValue(a.getDataFormat());
                    formatCell.setCellStyle(wrapStyle);
                }
            }
        }

        // Set column width
        dataFieldsSheet.autoSizeColumn(DATA_FIELDS_COLUMNS.NAME);
        dataFieldsSheet.setColumnWidth(DATA_FIELDS_COLUMNS.DEFINITION, 60 * 256);
        dataFieldsSheet.setColumnWidth(DATA_FIELDS_COLUMNS.CONTROLLED_VOCABULARY, 35 * 256);
        dataFieldsSheet.setColumnWidth(DATA_FIELDS_COLUMNS.DATA_FORMAT, 25 * 256);

    }

    /**
     * This function creates a sheet called "Lists" and then creates the pertinent validations for each of the lists
     */

    private void createListsSheetAndValidations(List<WriterWorksheet> sheets) {
        // Integer for holding column index value
        int column;
        // Create a sheet to hold the lists
        XSSFSheet listsSheet = workbook.createSheet(LISTS_SHEET_NAME);

        // Track which column number we're looking at
        int listColumnNumber = 0;


        for (biocode.fims.config.models.List list : project.getProjectConfig().lists()) {

            // List of fields from this validation rule
            List<Field> fields = list.getFields();

            // Validation Fields
            if (fields.size() > 0) {

                // populate this validation list in the Lists sheet
                int counterForRows = 0;
                for (Field f : fields) {
                    // Write header
                    if (counterForRows == 0) {
                        XSSFRow row = listsSheet.getRow(counterForRows);

                        if (row == null) {
                            row = listsSheet.createRow(counterForRows);
                        }

                        XSSFCell cell = row.createCell(listColumnNumber);
                        cell.setCellValue(list.getAlias());
                        cell.setCellStyle(headingStyle);
                    }

                    // Write cell values

                    // Set the row counter to +1 because of the header issues
                    counterForRows++;
                    XSSFRow row = listsSheet.getRow(counterForRows);
                    if (row == null) {
                        row = listsSheet.createRow(counterForRows);
                    }

                    XSSFCell cell = row.createCell(listColumnNumber);
                    cell.setCellValue(f.getValue());
                    cell.setCellStyle(regularStyle);

                }

                // Autosize this column
                listsSheet.autoSizeColumn(listColumnNumber);

                // Get the letter of this column
                String listColumnLetter = CellReference.convertNumToColString(listColumnNumber);

                // Figure out the last row number
                int endRowNum = fields.size() + 1;

                // DATA VALIDATION COMPONENT
                for (WriterWorksheet sheet : sheets) {
                    List<ControlledVocabularyRule> vocabularyRules = vocabRules(sheet.sheetName);

                    List<ControlledVocabularyRule> rules = vocabularyRules.stream()
                            .filter(r -> r.listName().equals(list.getAlias()))
                            .collect(Collectors.toList());

                    for (ControlledVocabularyRule r : rules) {
                        column = sheet.columns.indexOf(r.column());
                        if (column > -1) {

                            // Set the Constraint to a particular column on the lists sheet
                            // The following syntax works well and shows popup boxes: Lists!S:S
                            // replacing the previous syntax which does not show popup boxes ListsS
                            // Assumes that header is in column #1
                            String constraintSyntax = LISTS_SHEET_NAME + "!$" + listColumnLetter + "$2:$" + listColumnLetter + "$" + endRowNum;

                            DataValidationHelper dvHelper = listsSheet.getDataValidationHelper();

                            XSSFDataValidationConstraint dvConstraint =
                                    (XSSFDataValidationConstraint) dvHelper.createFormulaListConstraint(constraintSyntax);

                            // This defines an address range for this particular list
                            CellRangeAddressList addressList = new CellRangeAddressList();
                            addressList.addCellRangeAddress(1, column, 50000, column);

                            XSSFDataValidation dataValidation =
                                    (XSSFDataValidation) dvHelper.createValidation(dvConstraint, addressList);

                            // Data validation styling
                            dataValidation.setSuppressDropDownArrow(true);
                            dataValidation.setShowErrorBox(true);
                            // Give the user the appropriate data validation error msg, depending upon the rules error level
                            if (r.level().equals(RuleLevel.ERROR)) {
                                dataValidation.createErrorBox("Data Validation Error", ERROR_MSG);
                                dataValidation.setErrorStyle(DataValidation.ErrorStyle.WARNING);
                            } else {
                                dataValidation.createErrorBox("Data Validation Warning", WARNING_MSG);
                                dataValidation.setErrorStyle(DataValidation.ErrorStyle.INFO);
                            }
                            // Add the validation to the worksheet
                            workbook.getSheet(sheet.sheetName).addValidationData(dataValidation);
                        }
                    }
                }
                listColumnNumber++;
            }
        }
    }


    private List<ControlledVocabularyRule> vocabRules(String sheetName) {
        return project.getProjectConfig().entitiesForSheet(sheetName)
                .stream()
                .flatMap(e -> e.getRules().stream())
                .filter(ControlledVocabularyRule.class::isInstance)
                .map(ControlledVocabularyRule.class::cast)
                .collect(Collectors.toList());
    }


    private void initWorkbookStyles() {
        // Set the default heading style
        headingStyle = workbook.createCellStyle();
        Font bold = workbook.createFont();
        bold.setBold(true);
        bold.setFontHeightInPoints((short) 14);
        headingStyle.setFont(bold);


        requiredStyle = workbook.createCellStyle();
        Font redBold = workbook.createFont();
        redBold.setBold(true);
        redBold.setFontHeightInPoints((short) 14);
        redBold.setColor(XSSFFont.COLOR_RED);
        requiredStyle.setFont(redBold);

        wrapStyle = workbook.createCellStyle();
        wrapStyle.setWrapText(true);
        wrapStyle.setVerticalAlignment(VerticalAlignment.TOP);

        // Set the style for all other cells
        regularStyle = workbook.createCellStyle();
    }

    private class DATA_FIELDS_COLUMNS {
        private static final int NAME = 0;
        private static final int DEFINITION = 1;
        private static final int CONTROLLED_VOCABULARY = 2;
        private static final int DATA_FORMAT = 3;
    }
}

