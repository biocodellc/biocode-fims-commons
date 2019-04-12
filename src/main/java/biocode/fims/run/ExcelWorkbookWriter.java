package biocode.fims.run;

import biocode.fims.config.Config;
import biocode.fims.config.models.Attribute;
import biocode.fims.config.models.Field;
import biocode.fims.fimsExceptions.FimsRuntimeException;
import biocode.fims.fimsExceptions.errorCodes.FileCode;
import biocode.fims.models.Project;
import biocode.fims.models.User;
import biocode.fims.query.writers.WriterWorksheet;
import biocode.fims.utils.FileUtils;
import biocode.fims.validation.rules.ControlledVocabularyRule;
import biocode.fims.validation.rules.RuleLevel;
import org.dhatim.fastexcel.*;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
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

    private final static String RED_FONT = "FF2600";

    private final Project project;
    private final int naan;
    private final User user;
    private final Config config;

    /**
     * Constructor for writing a network based workbook. If there is
     * only a single project, use the ExcelWorkbookWriter(Project, int, User)
     * constructor
     *
     * @param config
     * @param naan
     */
    public ExcelWorkbookWriter(Config config, int naan) {
        this.config = config;
        this.naan = naan;
        this.project = null;
        this.user = null;
    }

    /**
     * Constructor for writing a project specific workbook
     *
     * @param project
     * @param naan
     * @param user
     */
    public ExcelWorkbookWriter(Project project, int naan, User user) {
        this.project = project;
        this.config = project.getProjectConfig();
        this.naan = naan;
        this.user = user;
    }

    public File write(List<WriterWorksheet> sheets) {
        // Create the output Filename and Write Excel File
        String filename = ((project == null) ? "workbook" : project.getProjectTitle()) + ".xlsx";
        File file = FileUtils.createUniqueFile(filename, System.getProperty("java.io.tmpdir"));
        try (OutputStream os = new FileOutputStream(file)) {
            Workbook workbook = new Workbook(os, "MyApplication", "1.0");

            List<CompletableFuture<Void>> cfs = new ArrayList<>();

            // create Worksheets in order we want the output
            // we asynchronously populate the sheet data

            // Create the Instructions Sheet, which is always first
            Worksheet ws = workbook.newWorksheet(INSTRUCTIONS_SHEET_NAME);
            CompletableFuture<Void> cf = CompletableFuture.runAsync(() -> createInstructions(
                    sheets.stream()
                            .map(s -> s.sheetName)
                            .collect(Collectors.toList()),
                    ws
            ));
            cfs.add(cf);


            List<Worksheet> dataSheets = new ArrayList<>();
            for (WriterWorksheet sheet : sheets) {
                Worksheet dataWs = workbook.newWorksheet(sheet.sheetName);
                dataSheets.add(dataWs);
                cf = CompletableFuture.runAsync(() -> writeDataSheet(sheet, dataWs));
                cfs.add(cf);
            }

            for (WriterWorksheet sheet : sheets) {
                Worksheet dataFieldsSheet = workbook.newWorksheet(sheet.sheetName + "_" + DATA_FIELDS_SHEET_NAME);
                cf = CompletableFuture.runAsync(() -> createDataFields(sheet.sheetName, sheet.columns, dataFieldsSheet));
                cfs.add(cf);
            }

            Worksheet listsSheet = workbook.newWorksheet(LISTS_SHEET_NAME);
            cf = CompletableFuture.runAsync(() -> createListsSheetAndValidations(sheets, listsSheet, dataSheets));
            cfs.add(cf);

            @SuppressWarnings("unchecked")
            CompletableFuture<Void>[] fs = new CompletableFuture[cfs.size()];
            // wait for all futures to complete
            CompletableFuture.allOf(cfs.toArray(fs)).get();

            workbook.finish();
        } catch (InterruptedException | ExecutionException | IOException e) {
            throw new FimsRuntimeException(FileCode.WRITE_ERROR, 500);
        }

        return file;
    }

    /**
     * Create an instructions sheet
     */
    private void createInstructions(List<String> worksheets, Worksheet instructionsSheet) {

        // Make a big first column
        instructionsSheet.width(0, 160);

        // Hide the projectId in the first row
        int row = 2;

        // Hide NAAN in first row, first column
        instructionsSheet.value(0, 0, "~naan=" + naan + "~");
        instructionsSheet.hideRow(0);

        StyleSetter styleSetter;
        if (project != null) {
            // The name of this project as specified by the sheet
            instructionsSheet.value(row, 0, project.getProjectTitle());
            styleSetter = instructionsSheet.style(row, 0);
            styleHeading(styleSetter, false);
            styleCentered(styleSetter);
        }
        row++;


        // Print todays date with user name
        DateFormat dateFormat = new SimpleDateFormat("MMMM dd, yyyy");
        Calendar cal = Calendar.getInstance();
        String dateAndUser = ((project == null) ? "Workbook" : "Template") + " generated ";
        if (user != null) {
            dateAndUser += "by '" + user.getUsername() + "' ";
        }
        dateAndUser += "on " + dateFormat.format(cal.getTime());
        instructionsSheet.value(row, 0, dateAndUser);

        styleSetter = instructionsSheet.style(row, 0);
        styleHeading(styleSetter, false);
        styleCentered(styleSetter);
        row++;

        instructionsSheet.value(row, 0, "Person(s) responsible for data entry [                       ]");
        styleSetter = instructionsSheet.style(row, 0);
        styleHeading(styleSetter, false);
        styleCentered(styleSetter);
        row++;

        // Insert additional row before next content
        row++;

        // worksheet instructions

        instructionsSheet.value(row, 0,
                worksheets.stream()
                        .collect(Collectors.joining(", "))
                        .concat(worksheets.size() > 1 ? " Tabs" : " Tab")
        );
        styleHeading(instructionsSheet.style(row, 0));
        row++;

        styleWrapped(instructionsSheet.style(row, 0));
        String joinedSheets = worksheets.stream()
                .collect(Collectors.joining("\", \""))
                .concat(worksheets.size() > 1 ? " tabs" : " tab");
        instructionsSheet.value(row, 0, "Please fill out each field in the " + joinedSheets + " as completely as possible. " +
                "Fields in red are required (data cannot be uploaded to the database without these fields). " +
                "Required and recommended fields are usually placed towards the beginning of the template. " +
                "Some fields have a controlled vocabulary associated with them in the \"" + LISTS_SHEET_NAME + "\" tab " +
                "and are provided as data validation in the provided cells" +
                "If you have more than one entry to a field (i.e. a list of publications), " +
                "please delimit your list with pipes (|).  Also please make sure that there are no newline " +
                "characters (=carriage returns) in any of your metadata. Fields in the " + joinedSheets + " may be re-arranged " +
                "in any order so long as you don't change the field names.");
        row++;
        row++;

        // data Fields sheet
        styleHeading(instructionsSheet.style(row, 0));
        instructionsSheet.value(row, 0, worksheets.stream()
                .map(s -> s + "_" + DATA_FIELDS_SHEET_NAME)
                .collect(Collectors.joining(", "))
                .concat(worksheets.size() > 1 ? " Tabs" : " Tab")
        );
        row++;

        styleWrapped(instructionsSheet.style(row, 0));
        instructionsSheet.value(row, 0, "This tab contains column names, associated URIs and definitions for each column.");
        row++;
        row++;

        //Lists Tab
        styleHeading(instructionsSheet.style(row, 0));
        instructionsSheet.value(row, 0, LISTS_SHEET_NAME + " Tab");
        row++;

        styleWrapped(instructionsSheet.style(row, 0));
        instructionsSheet.value(row, 0, "This tab contains controlled vocabulary lists for certain fields.  DO NOT EDIT this sheet!");

    }

    private void writeDataSheet(WriterWorksheet sheet, Worksheet worksheet) {
        //Create the header row
        writeHeaderRow(sheet, worksheet);

        int rowNum = 1;
        for (Map<String, Object> record : sheet.data) {
            addDataToRow(sheet, record, worksheet, rowNum);
            rowNum++;
        }
    }

    private void writeHeaderRow(WriterWorksheet sheet, Worksheet worksheet) {
        // First find all the required columns so we can look them up
        Set<String> requiredColumns = this.config.getRequiredColumns(sheet.sheetName, RuleLevel.ERROR);

        int col = 0;
        for (String column : sheet.columns) {
            worksheet.value(0, col, column);

            StyleSetter style = worksheet.style(0, col);
            // Make required columns red
            if (requiredColumns.contains(column)) {
                styleRequired(style, false);
            }
            styleHeading(style);
            col++;
        }
    }


    private void addDataToRow(WriterWorksheet sheet, Map<String, Object> record, Worksheet worksheet, int row) {
        int cellNum = 0;

        for (String column : sheet.columns) {
            Object val = record.get(column);

            if (val != null && !val.equals("")) {
                worksheet.value(row, cellNum, val);
            }

            cellNum++;
        }
    }

    /**
     * Create the DataFields sheet
     */
    private void createDataFields(String sheetName, List<String> columns, Worksheet dataFieldsSheet) {
        // First find all the required columns so we can look them up
        Set<String> requiredColumns = this.config.getRequiredColumns(sheetName, RuleLevel.ERROR);

        int row = 0;

        // HEADER ROWS
        dataFieldsSheet.value(row, DATA_FIELDS_COLUMNS.NAME, "ColumnName");
        dataFieldsSheet.value(row, DATA_FIELDS_COLUMNS.DEFINITION, "Definition");
        dataFieldsSheet.value(row, DATA_FIELDS_COLUMNS.CONTROLLED_VOCABULARY, "Controlled Vocabulary (see Lists)");
        dataFieldsSheet.value(row, DATA_FIELDS_COLUMNS.DATA_FORMAT, "Data Format");

        styleHeading(dataFieldsSheet.range(0, 0, 0, 3).style());

        List<ControlledVocabularyRule> vocabularyRules = vocabRules(sheetName);

        row++;

        for (Attribute a : config.attributesForSheet(sheetName)) {

            if (columns.contains(a.getColumn())) {

                // Column Name
                dataFieldsSheet.value(row, DATA_FIELDS_COLUMNS.NAME, a.getColumn());
                if (requiredColumns.contains(a.getColumn())) {
                    styleRequired(dataFieldsSheet.style(row, DATA_FIELDS_COLUMNS.NAME));
                } else {
                    styleHeading(dataFieldsSheet.style(row, DATA_FIELDS_COLUMNS.NAME));
                }

                // Definition
                if (a.getDefinition() != null) {
                    dataFieldsSheet.value(row, DATA_FIELDS_COLUMNS.DEFINITION, a.getDefinition());
                    styleWrapped(dataFieldsSheet.style(row, DATA_FIELDS_COLUMNS.DEFINITION));
                }

                // Controlled Vocabulary
                for (ControlledVocabularyRule r : vocabularyRules) {
                    if (r.column().equals(a.getColumn())) {
                        dataFieldsSheet.value(row, DATA_FIELDS_COLUMNS.CONTROLLED_VOCABULARY, r.listName());
                        styleWrapped(dataFieldsSheet.style(row, DATA_FIELDS_COLUMNS.CONTROLLED_VOCABULARY));
                        break;
                    }
                }

                // Data Format
                if (a.getDataFormat() != null) {
                    dataFieldsSheet.value(row, DATA_FIELDS_COLUMNS.DATA_FORMAT, a.getDataFormat());
                    styleWrapped(dataFieldsSheet.style(row, DATA_FIELDS_COLUMNS.DATA_FORMAT));
                }

                row++;
            }
        }

        // Set column width
        dataFieldsSheet.width(DATA_FIELDS_COLUMNS.DEFINITION, 60);
        dataFieldsSheet.width(DATA_FIELDS_COLUMNS.CONTROLLED_VOCABULARY, 35);
        dataFieldsSheet.width(DATA_FIELDS_COLUMNS.DATA_FORMAT, 25);

    }


    /**
     * This function creates a sheet called "Lists" and then creates the pertinent validations for each of the lists
     */

    private void createListsSheetAndValidations(List<WriterWorksheet> sheets, Worksheet listsSheet, List<Worksheet> dataSheets) {
        // Track which column number we're looking at
        int col = 0;

        for (biocode.fims.config.models.List list : config.lists()) {

            // List of fields from this validation rule
            List<Field> fields = list.getFields();

            // Validation Fields
            if (fields.size() > 0) {

                // populate this validation list in the Lists sheet
                int row = 0;
                for (Field f : fields) {
                    // Write header
                    if (row == 0) {
                        listsSheet.value(row, col, list.getAlias());
                        styleHeading(listsSheet.style(row, col));
                    }

                    // Write cell values
                    row++;
                    listsSheet.value(row, col, f.getValue());

                }

                Range listRange = listsSheet.range(1, col, fields.size(), col);

                // DATA VALIDATION COMPONENT
                for (WriterWorksheet sheet : sheets) {
                    List<ControlledVocabularyRule> vocabularyRules = vocabRules(sheet.sheetName);

                    List<ControlledVocabularyRule> rules = vocabularyRules.stream()
                            .filter(r -> r.listName().equals(list.getAlias()))
                            .collect(Collectors.toList());

                    Worksheet ws = dataSheets.stream().filter(s -> s.getName().equals(sheet.sheetName)).findFirst().get();

                    for (ControlledVocabularyRule r : rules) {
                        int column = sheet.columns.indexOf(r.column());
                        if (column > -1) {

                            // This defines an address range we want to place the DataValidation on

                            ListDataValidation dataValidation = ws.range(1, column, 100000, column).validateWithList(listRange);


                            // Data validation styling
                            dataValidation
                                    .showDropdown(true)
                                    .showErrorMessage(true);
                            // Give the user the appropriate data validation error msg, depending upon the rules error level
                            if (r.level().equals(RuleLevel.ERROR)) {
                                dataValidation
                                        .errorTitle("Data Validation Error")
                                        .error(ERROR_MSG)
                                        .errorStyle(DataValidationErrorStyle.STOP);
                            } else {
                                dataValidation
                                        .errorTitle("Data Validation Warning")
                                        .error(WARNING_MSG)
                                        .errorStyle(DataValidationErrorStyle.INFORMATION);
                            }
                        }
                    }
                }
                col++;
            }
        }
    }


    private List<ControlledVocabularyRule> vocabRules(String sheetName) {
        return config.entitiesForSheet(sheetName)
                .stream()
                .flatMap(e -> e.getRules().stream())
                .filter(ControlledVocabularyRule.class::isInstance)
                .map(ControlledVocabularyRule.class::cast)
                .collect(Collectors.toList());
    }


    private void styleHeading(StyleSetter styleSetter) {
        styleHeading(styleSetter, true);
    }

    private void styleHeading(StyleSetter styleSetter, boolean set) {
        styleSetter.bold().fontSize(14);
        if (set) styleSetter.set();
    }

    private void styleRequired(StyleSetter styleSetter) {
        styleRequired(styleSetter, true);
    }

    private void styleRequired(StyleSetter styleSetter, boolean set) {
        styleHeading(styleSetter);
        styleSetter.fontColor(RED_FONT);
        if (set) styleSetter.set();
    }

    private void styleWrapped(StyleSetter styleSetter) {
        styleWrapped(styleSetter, true);
    }

    private void styleWrapped(StyleSetter styleSetter, boolean set) {
        styleSetter.wrapText(true).verticalAlignment("Top");
        if (set) styleSetter.set();
    }

    private void styleCentered(StyleSetter styleSetter) {
        styleCentered(styleSetter, true);
    }

    private void styleCentered(StyleSetter styleSetter, boolean set) {
        styleSetter.horizontalAlignment("center");
        if (set) styleSetter.set();
    }

    private class DATA_FIELDS_COLUMNS {
        private static final int NAME = 0;
        private static final int DEFINITION = 1;
        private static final int CONTROLLED_VOCABULARY = 2;
        private static final int DATA_FORMAT = 3;
    }
}

