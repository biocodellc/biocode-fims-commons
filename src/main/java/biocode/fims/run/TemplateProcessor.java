package biocode.fims.run;

import biocode.fims.config.ConfigurationFileFetcher;
import biocode.fims.digester.*;
import biocode.fims.entities.Project;
import biocode.fims.fimsExceptions.FimsRuntimeException;
import biocode.fims.service.ExpeditionService;
import biocode.fims.settings.PathManager;
import biocode.fims.settings.SettingsManager;
import org.apache.commons.digester3.Digester;
import org.apache.poi.hssf.util.CellReference;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddressList;
import org.apache.poi.xssf.usermodel.*;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.List;

/**
 * This is a convenience class for working with templates (the spreadsheet generator).
 * We handle building working with the "process" class and the digester rules for mapping & fims,
 * in addition to providing methods for looking up definitions, and building form output
 */
public class TemplateProcessor {

    private Mapping mapping;
    private Validation validation;
    private String accessionNumber;
    private String datasetCode;
    private Integer naan;

    private String identifier;

    private static Logger logger = LoggerFactory.getLogger(TemplateProcessor.class);

    XSSFSheet defaultSheet;
    XSSFWorkbook workbook;

    XSSFCellStyle headingStyle, regularStyle, requiredStyle, wrapStyle;

    final int NAME = 0;
    final int DEFINITION = 1;
    final int CONTROLLED_VOCABULARY = 2;
    final int DATA_FORMAT = 3;
    final int SYNONYMS = 4;


    String instructionsSheetName = "Instructions";
    String dataFieldsSheetName = "Data Fields";
    String listsSheetName = "Lists";

    private static String warningMsg = "The value you entered is not from the recommended list. This will create a warning upon validation.";
    private static String errorMsg = "The value you entered is not from the recommended list. This will create an error upon validation.";

    static File configFile = null;
    Integer projectId;
    private String username = null;

    public TemplateProcessor(Integer projectId, String outputFolder, Boolean useCache, XSSFWorkbook workbook) {
        this.projectId = projectId;
        ConfigurationFileFetcher configFile = new ConfigurationFileFetcher(projectId, outputFolder, useCache);
        SettingsManager sm = SettingsManager.getInstance();
        naan = Integer.parseInt(sm.retrieveValue("naan"));

        mapping = new Mapping();
        mapping.addMappingRules(new Digester(), configFile.getOutputFile());

        validation = new Validation();
        validation.addValidationRules(new Digester(), configFile.getOutputFile(), mapping);

        this.workbook = (XSSFWorkbook) workbook;
        // Set the default heading style
        headingStyle = workbook.createCellStyle();
        XSSFFont bold = workbook.createFont();
        bold.setFontHeightInPoints((short) 14);
        bold.setBoldweight(XSSFFont.BOLDWEIGHT_BOLD);
        headingStyle.setFont(bold);


        requiredStyle = workbook.createCellStyle();
        XSSFFont redBold = workbook.createFont();
        redBold.setBoldweight(XSSFFont.BOLDWEIGHT_BOLD);
        redBold.setFontHeightInPoints((short) 14);
        redBold.setColor(XSSFFont.COLOR_RED);
        requiredStyle.setFont(redBold);

        wrapStyle = workbook.createCellStyle();
        wrapStyle.setWrapText(true);
        wrapStyle.setVerticalAlignment(CellStyle.VERTICAL_TOP);

        // Set the style for all other cells
        regularStyle = workbook.createCellStyle();

    }

    /**
     * Instantiate tempalateProcessor using a pre-defined configurationFile (don't fetch using projectID)
     * This is a private constructor as it is ONLY used for local testing.  Do not use on the Web or in production
     * since we MUST know the projectId first
     *
     * @param file
     */
    private void instantiateTemplateProcessor(File file) {
        configFile = file;

        SettingsManager sm = SettingsManager.getInstance();
        naan = Integer.parseInt(sm.retrieveValue("naan"));

        mapping = new Mapping();
        mapping.addMappingRules(new Digester(), configFile);

        validation = new Validation();
        validation.addValidationRules(new Digester(), configFile, mapping);

        // Create the workbook
        workbook = new XSSFWorkbook();

        // Set the default heading style
        headingStyle = workbook.createCellStyle();
        XSSFFont bold = workbook.createFont();
        bold.setFontHeightInPoints((short) 14);
        bold.setBoldweight(XSSFFont.BOLDWEIGHT_BOLD);
        headingStyle.setFont(bold);


        requiredStyle = workbook.createCellStyle();
        XSSFFont redBold = workbook.createFont();
        redBold.setBoldweight(XSSFFont.BOLDWEIGHT_BOLD);
        redBold.setFontHeightInPoints((short) 14);
        redBold.setColor(XSSFFont.COLOR_RED);
        requiredStyle.setFont(redBold);

        wrapStyle = workbook.createCellStyle();
        wrapStyle.setWrapText(true);
        wrapStyle.setVerticalAlignment(CellStyle.VERTICAL_TOP);

        // Set the style for all other cells
        regularStyle = workbook.createCellStyle();
    }

    /**
     * Instantiate TemplateProcessor using a project ID (lookup configuration File from server)
     *
     * @param projectId
     * @param outputFolder
     * @param useCache
     */
    public void instantiateTemplateProcessor(Integer projectId, String outputFolder, Boolean useCache) {
        this.projectId = projectId;
        ConfigurationFileFetcher fetcher = new ConfigurationFileFetcher(projectId, outputFolder, useCache);
        instantiateTemplateProcessor(fetcher.getOutputFile());
    }


    /**
     * constructor for NMNH projects
     *
     * @param projectId
     * @param outputFolder
     * @param useCache
     * @param accessionNumber
     * @param datasetCode
     */
    public TemplateProcessor(Integer projectId, String outputFolder, Boolean useCache,
                             String accessionNumber, String datasetCode, String identifier, String username) {
        // we can't have a null value for accessionNumber or datasetCode if using this constructor
        if (accessionNumber == null || datasetCode == null) {
            throw new FimsRuntimeException("dataset code and accession number are required", 500);
        }
        this.username = username;
        this.accessionNumber = accessionNumber;
        this.datasetCode = datasetCode;
        this.identifier = identifier;
        instantiateTemplateProcessor(projectId, outputFolder, useCache);
    }

    /**
     * constructor for NMNH projects
     *
     * @param file
     * @param accessionNumber
     * @param datasetCode
     */
    public TemplateProcessor(File file, String accessionNumber, String datasetCode, String identifier) {
        // we can't have a null value for accessionNumber or datasetCode if using this constructor
        if (accessionNumber == null || datasetCode == null) {
            throw new FimsRuntimeException("dataset code and accession number are required", 500);
        }
        this.accessionNumber = accessionNumber;
        this.datasetCode = datasetCode;
        this.identifier = identifier;
        instantiateTemplateProcessor(file);
    }

    public TemplateProcessor(Integer projectId, String outputFolder, Boolean useCache) {
        instantiateTemplateProcessor(projectId, outputFolder, useCache);
    }

    public Mapping getMapping() {
        return mapping;
    }

    public Validation getValidation() {
        return validation;
    }

    /**
     * Get definitions for a particular column name
     *
     * @param columnName
     *
     * @return
     */
    public JSONObject getDefinition(String columnName) {
        //TODO should this be in mapping?
        Iterator attributes = mapping.getAllAttributes(mapping.getDefaultSheetName()).iterator();
        // Get a list of rules for the first digester.Worksheet instance
        Worksheet sheet = this.validation.getWorksheets().get(0);

        List<Rule> rules = sheet.getRules();


        while (attributes.hasNext()) {
            Attribute a = (Attribute) attributes.next();
            String column = a.getColumn();

            if (columnName.trim().equals(column.trim())) {
                JSONObject definition = new JSONObject();

                definition.put("columnName", column);
                definition.put("uri", (a.getUri() != null) ? a.getUri() : "");
                definition.put("defined_by", (a.getDefined_by() != null) ? a.getDefined_by() : "");
                definition.put("definition", (a.getDefinition() != null) ? a.getDefinition() : "No custom definition available.");
                definition.put("synonyms", (a.getSynonyms() != null) ? a.getSynonyms() : "");
                definition.put("dataFormat", (a.getDataformat() != null) ? a.getDataformat() : "");

                // Rules
                Iterator it = rules.iterator();
                JSONArray ruleValidations = new JSONArray();
                while (it.hasNext()) {

                    Rule r = (Rule) it.next();
                    r.setDigesterWorksheet(sheet);

                    if (r != null) {
                        biocode.fims.digester.List sList = validation.findList(r.getList());

                        // Convert to native state (without underscores)
                        String ruleColumn = r.getColumn();

                        if (ruleColumn != null) {
                            // Match column names with or without underscores
                            if (ruleColumn.replace("_", " ").equals(column) ||
                                    ruleColumn.equals(column)) {
                                ruleValidations.add(r.getRuleMetadata(sList));
                            }
                        }
                    }
                }
                definition.put("rules", ruleValidations);
                return definition;
            }
        }
        return new JSONObject();
    }

    /**
     * Generate checkBoxes/Column Names for the mappings in a template
     *
     * @return
     */
    public JSONObject getAttributesByGroup() {
        LinkedList<String> requiredColumns = getRequiredColumns("error");
        LinkedList<String> desiredColumns = getRequiredColumns("warning");
        JSONObject attributesByGroup = new JSONObject();

        // A list of names we've already added
        ArrayList addedNames = new ArrayList();
        Iterator attributes = mapping.getAllAttributes(mapping.getDefaultSheetName()).iterator();

        while (attributes.hasNext()) {
            Attribute a = (Attribute) attributes.next();

            // Set the column and group
            String aColumn = a.getColumn();
            String aGroup = a.getGroup();

            // set the default group
            if (aGroup == null || aGroup.equals("")) {
                aGroup = "Default Group";
            }

            // Check that this name hasn't been read already.  This is necessary in some situations where
            // column names are repeated for different entities in the configuration file
            if (!addedNames.contains(aColumn)) {
                JSONObject columnMetadata = new JSONObject();
                JSONObject column = new JSONObject();

                // Determine if this is a required or desired column
                if (requiredColumns != null && requiredColumns.contains(aColumn)) {
                    columnMetadata.put("level", "required");
                } else if (desiredColumns == null && desiredColumns.contains(aColumn)) {
                    columnMetadata.put("level", "desired");
                } else {
                    columnMetadata.put("level", "default");
                }

                // add column metadata (uri & level)
                columnMetadata.put("uri", a.getUri());

                // add column metadata to the column object
                column.put(aColumn, columnMetadata);

                if (!attributesByGroup.containsKey(aGroup)) {
                    JSONArray columns = new JSONArray();
                    columns.add(column);
                    attributesByGroup.put(aGroup, columns);
                } else {
                    JSONArray columns = (JSONArray) attributesByGroup.get(aGroup);
                    columns.add(column);
                }

            }

            // Now that we've added this to the output, add it to the ArrayList so we don't add it again
            addedNames.add(aColumn);
        }

        return attributesByGroup;
    }

    /**
     * This function creates a sheet called "Lists" and then creates the pertinent validations for each of the lists
     *
     * @param fields
     */

    private void createListsSheetAndValidations(List<String> fields) {
        // Integer for holding column index value
        int column;
        // Create a sheet to hold the lists
        XSSFSheet listsSheet = workbook.createSheet(listsSheetName);

        // An iterator of the possible lists
        Iterator listsIt = validation.getLists().iterator();

        // Track which column number we're looking at
        int listColumnNumber = 0;

        // Loop our array of lists
        while (listsIt.hasNext()) {
            // Get an instance of a particular list
            biocode.fims.digester.List list = (biocode.fims.digester.List) listsIt.next();

            //Get the number of rows in this list
            int numRowsInList = list.getFields().size();

            // List of fields from this validation rule
            List validationFieldList = list.getFields();

            // Validation Fields
            if (validationFieldList.size() > 0) {

                // populate this validation list in the Lists sheet
                int counterForRows = 0;
                Iterator fieldlistIt = validationFieldList.iterator();
                while (fieldlistIt.hasNext()) {
                    String value;
                    XSSFCellStyle style;
                    // Write header
                    if (counterForRows == 0) {
                        value = list.getAlias();
                        style = headingStyle;
                        XSSFRow row = listsSheet.getRow(counterForRows);
                        if (row == null)
                            row = listsSheet.createRow(counterForRows);

                        XSSFCell cell = row.createCell(listColumnNumber);
                        cell.setCellValue(value);
                        cell.setCellStyle(style);
                    }
                    // Write cell values
                    Field f = (Field) fieldlistIt.next();
                    value = f.getValue();

                    style = regularStyle;

                    // Set the row counter to +1 because of the header issues
                    counterForRows++;
                    XSSFRow row = listsSheet.getRow(counterForRows);
                    if (row == null)
                        row = listsSheet.createRow(counterForRows);

                    XSSFCell cell = row.createCell(listColumnNumber);
                    cell.setCellValue(value);
                    cell.setCellStyle(style);

                }

                // Autosize this column
                listsSheet.autoSizeColumn(listColumnNumber);

                // Get the letter of this column
                String listColumnLetter = CellReference.convertNumToColString(listColumnNumber);

                // Figure out the last row number
                int endRowNum = numRowsInList + 1;

                // DATA VALIDATION COMPONENT
                // TODO: expand this to select the appropriate worksheet but for NOW there is only ONE so just get last
                Worksheet validationWorksheet = validation.getWorksheets().getLast();
                // An arrayList of columnNames in the default sheet that this list should be applied to
                ArrayList<String> columnNames = validationWorksheet.getColumnsForList(list.getAlias());

                // Determine if the list will throw a warning or an error message upon validation
                List<Rule> rules = validationWorksheet.getRules();
                Boolean errorLevel = false;
                Iterator rulesIt = rules.iterator();

                while (rulesIt.hasNext()) {
                    Rule r = (Rule) rulesIt.next();
                    if (r.getList() != null && r.getList().equalsIgnoreCase(list.getAlias())) {
                        if (r.getLevel().equalsIgnoreCase("warning")) {
                            errorLevel = true;
                        }
                    }
                }


                Iterator columnNamesIt = columnNames.iterator();
                // Loop all of the columnNames
                while (columnNamesIt.hasNext()) {
                    String thisColumnName = (String) columnNamesIt.next();
                    column = fields.indexOf(thisColumnName.replace("_", " "));
                    if (column >= 0) {
                        ///   CellRangeAddressList addressList = new CellRangeAddressList(1, 100000, 2, 2);

                        // Set the Constraint to a particular column on the lists sheet
                        // The following syntax works well and shows popup boxes: Lists!S:S
                        // replacing the previous syntax which does not show popup boxes ListsS
                        // Assumes that header is in column #1
                        String constraintSyntax = listsSheetName + "!$" + listColumnLetter + "$2:$" + listColumnLetter + "$" + endRowNum;

                        XSSFDataValidationHelper dvHelper =
                                new XSSFDataValidationHelper(listsSheet);

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
                        if (errorLevel) {
                            dataValidation.createErrorBox("Data Validation Error", errorMsg);
                            dataValidation.setErrorStyle(DataValidation.ErrorStyle.WARNING);
                        } else {
                            dataValidation.createErrorBox("Data Validation Warning", warningMsg);
                            dataValidation.setErrorStyle(DataValidation.ErrorStyle.INFO);
                        }

                        // Add the validation to the defaultsheet
                        defaultSheet.addValidationData(dataValidation);
                    }
                }
                listColumnNumber++;
            }
        }
    }


    /**
     * Find the required columns on this sheet
     *
     * @return
     */
    public LinkedList<String> getRequiredColumns(String level) {
        LinkedList<String> columnSet = new LinkedList<String>();
        Iterator worksheetsIt = validation.getWorksheets().iterator();
        while (worksheetsIt.hasNext()) {
            Worksheet w = (Worksheet) worksheetsIt.next();
            Iterator rIt = w.getRules().iterator();
            while (rIt.hasNext()) {
                Rule r = (Rule) rIt.next();
                //System.out.println(r.getType() + r.getColumn() + r.getFields());
                if (r.getType().equals("RequiredColumns") &&
                        r.getLevel().equals(level)) {
                    columnSet.addAll(r.getFields());
                }
            }
        }
        if (columnSet.size() < 1)
            return null;
        else
            return columnSet;
    }

    /**
     * Create the DataFields sheet
     */
    private void createDataFields(List<String> fields) {

        // Create the Instructions Sheet, which is always first
        XSSFSheet dataFieldsSheet = workbook.createSheet(dataFieldsSheetName);

        // First find all the required columns so we can look them up
        LinkedList<String> requiredColumns = getRequiredColumns("error");


        // Loop through all fields in schema and provide names, uris, and definitions
        //Iterator entitiesIt = getMapping().getEntities().iterator();
        Iterator fieldsIt = fields.iterator();
        int rowNum = 0;
        Row row = dataFieldsSheet.createRow(rowNum++);

        //XSSFCellStyle dataFieldStyle = headingStyle;
        //dataFieldStyle.setBorderBottom(BorderStyle.THIN);

        // HEADER ROWS
        Cell cell = row.createCell(NAME);
        cell.setCellStyle(headingStyle);
        cell.setCellValue("ColumnName");

        cell = row.createCell(DEFINITION);
        cell.setCellStyle(headingStyle);
        cell.setCellValue("Definition");

        cell = row.createCell(CONTROLLED_VOCABULARY);
        cell.setCellStyle(headingStyle);
        cell.setCellValue("Controlled Vocabulary (see Lists)");

        cell = row.createCell(DATA_FORMAT);
        cell.setCellStyle(headingStyle);
        cell.setCellValue("Data Format");

        cell = row.createCell(SYNONYMS);
        cell.setCellStyle(headingStyle);
        cell.setCellValue("Synonyms");

        // Must loop entities first
        while (fieldsIt.hasNext()) {
            // Generally, treat column Names with underscores or not, replacing spaces
            String columnName = fieldsIt.next().toString().replace("_", " ");
            LinkedList<Entity> entities = mapping.getEntities();
            Iterator entitiesIt = entities.iterator();
            while (entitiesIt.hasNext()) {
                Entity e = (Entity) entitiesIt.next();

                // Loop attributes
                Iterator attributesIt = ((LinkedList<Attribute>) e.getAttributes()).iterator();

                // Then loop attributes
                while (attributesIt.hasNext()) {

                    Attribute a = (Attribute) attributesIt.next();

                    // Some XML configuration files allow spaces in column Names... here we search for
                    // matching column names with or without spaces, replaced by underscores
                    if (a.getColumn().replace("_", " ").equals(columnName) ||
                            a.getColumn().equals(columnName)) {
                        row = dataFieldsSheet.createRow(rowNum++);

                        // Column Name
                        Cell nameCell = row.createCell(NAME);
                        nameCell.setCellValue(a.getColumn());
                        XSSFCellStyle nameStyle;
                        if (requiredColumns != null && requiredColumns.contains(a.getColumn())) {
                            nameStyle = requiredStyle;
                        } else {
                            nameStyle = headingStyle;
                        }
                        nameStyle.setVerticalAlignment(CellStyle.VERTICAL_TOP);
                        nameCell.setCellStyle(nameStyle);

                        // Definition
                        Cell defCell = row.createCell(DEFINITION);
                        defCell.setCellValue(a.getDefinition());
                        defCell.setCellStyle(wrapStyle);

                        // Controlled Vocabulary
                        Worksheet sheet = this.validation.getWorksheets().get(0);
                        Iterator rulesIt = sheet.getRules().iterator();
                        while (rulesIt.hasNext()) {
                            Rule r = (Rule) rulesIt.next();
                            if (r.getColumn() != null &&
                                    r.getList() != null &&
                                    r.getColumn().replace("_", " ").equals(columnName) &&
                                    (r.getType().equals("controlledVocabulary") || r.getType().equals("checkInXMLFields"))) {
                                Cell controlledVocabCell = row.createCell(CONTROLLED_VOCABULARY);
                                controlledVocabCell.setCellValue(r.getList());
                                controlledVocabCell.setCellStyle(wrapStyle);
                            }
                        }

                        // Data Format
                        try {
                            Cell formatCell = row.createCell(DATA_FORMAT);
                            formatCell.setCellValue(a.getDataformat());
                            formatCell.setCellStyle(wrapStyle);
                        } catch (NullPointerException npe) {
                            logger.warn("NullPointerException", npe);
                        }

                        // Synonyms
                        try {
                            Cell synonymCell = row.createCell(SYNONYMS);
                            synonymCell.setCellValue(a.getSynonyms());
                            synonymCell.setCellStyle(wrapStyle);
                        } catch (NullPointerException npe) {
                            logger.warn("NullPointerException", npe);
                        }
                    }
                }
            }
        }

        // Set column width
        dataFieldsSheet.setColumnWidth(NAME, 25 * 256);
        dataFieldsSheet.setColumnWidth(DEFINITION, 60 * 256);
        dataFieldsSheet.setColumnWidth(CONTROLLED_VOCABULARY, 25 * 256);
        dataFieldsSheet.setColumnWidth(DATA_FORMAT, 40 * 256);
        dataFieldsSheet.setColumnWidth(SYNONYMS, 40 * 256);

    }

    /**
     * Create the default Sheet
     *
     * @param defaultSheetname
     * @param fields
     */
    private void createDefaultSheet(String defaultSheetname, List<String> fields) {
        // Create the Default Sheet sheet
        defaultSheet = workbook.createSheet(defaultSheetname);

        //Create the header row
        XSSFRow row = defaultSheet.createRow(0);

        // First find all the required columns so we can look them up
        LinkedList<String> requiredColumns = getRequiredColumns("error");

        // Loop the fields that the user wants in the default sheet
        int columnNum = 0;
        Iterator itFields = fields.iterator();
        while (itFields.hasNext()) {
            String field = (String) itFields.next();
            Cell cell = row.createCell(columnNum++);
            //Set value to new value
            cell.setCellValue(field);
            cell.setCellStyle(headingStyle);

            // Make required columns red
            if (requiredColumns != null && requiredColumns.contains(field))
                cell.setCellStyle(requiredStyle);

        }

        // Auto-size the columns so we can see them all to begin with
        for (int i = 0; i <= columnNum; i++) {
            defaultSheet.autoSizeColumn(i);
        }

    }

    /**
     * Create an instructions sheet
     *
     * @param defaultSheetName
     */
    private void createInstructions(String defaultSheetName) {
        // Create the Instructions Sheet, which is always first
        XSSFSheet instructionsSheet = workbook.createSheet(instructionsSheetName);
        Row row;
        Cell cell;
        Integer rowIndex = 0;

        // Center align & bold for title
        XSSFCellStyle titleStyle = workbook.createCellStyle();
        XSSFFont bold = workbook.createFont();
        bold.setFontHeightInPoints((short) 14);

        bold.setBoldweight(XSSFFont.BOLDWEIGHT_BOLD);
        titleStyle.setFont(bold);
        titleStyle.setAlignment(CellStyle.ALIGN_CENTER);

        // Make a big first column
        instructionsSheet.setColumnWidth(0, 160 * 256);

        //Fetch the project title from the BCID system
        // NOTE, getting this particular name from the BCID system throws a connection exception
        /* AvailableProjectsFetcher fetcher = new AvailableProjectsFetcher();
        AvailableProject aP = fetcher.getProject(projectId);
        String projectTitle = aP.getProjectTitle();
        */
        // Use the shortName
        String projectTitle = mapping.getMetadata().getShortname();

        // Hide the projectId in the first row
        row = instructionsSheet.createRow(rowIndex);
        rowIndex++;
        rowIndex++;

        // Hide NAAN in first row, first column
        cell = row.createCell(0);
        cell.setCellValue("~naan=" + naan + "~");

        // Hide Project_id in first row, second column
        cell = row.createCell(1);
        cell.setCellValue("~project_id=" + projectId + "~");

        row.setZeroHeight(true);

        // The name of this project as specified by the sheet
        row = instructionsSheet.createRow(rowIndex);
        rowIndex++;
        cell = row.createCell(0);
        cell.setCellStyle(titleStyle);
        cell.setCellValue(projectTitle);

        // if we have a datasetCode and accessionNumber, hide them in the first row and make them visible
        // if we have one, we have all three.
        if (accessionNumber != null) {
            // Hide the dataset_code in first row, third column
            row = instructionsSheet.getRow(0);
            cell = row.createCell(2);
            cell.setCellValue("~dataset_code=" + datasetCode + "~");

            // Hide the accession number in first row, fourth column
            cell = row.createCell(3);
            cell.setCellValue("~accession_number=" + accessionNumber + "~");

            // Show the datasetCode
            row = instructionsSheet.createRow(rowIndex);
            rowIndex++;
            cell = row.createCell(0);
            cell.setCellStyle(titleStyle);
            cell.setCellValue(formatKeyValueString("Dataset Code: ", datasetCode));

            // Show the identifier
            if (identifier != null && !identifier.equals("")) {
                row = instructionsSheet.createRow(rowIndex);
                rowIndex++;
                cell = row.createCell(0);
                cell.setCellStyle(titleStyle);
                cell.setCellValue(formatKeyValueString("ARK root: ", identifier));
            }

            // Show the Accession Number
            row = instructionsSheet.createRow(rowIndex);
            rowIndex++;
            cell = row.createCell(0);
            cell.setCellStyle(titleStyle);
            cell.setCellValue(formatKeyValueString("Accession Number: ", accessionNumber));
        }


        // Print todays date with user name
        row = instructionsSheet.createRow(rowIndex);
        rowIndex++;
        cell = row.createCell(0);
        cell.setCellStyle(titleStyle);
        DateFormat dateFormat = new SimpleDateFormat("MMMMM dd, yyyy");
        Calendar cal = Calendar.getInstance();
        String dateAndUser = "Templated generated ";
        if (username != null && !username.trim().equals("")) {
            dateAndUser += "by '" + username + "' ";
        }
        dateAndUser += "on " + dateFormat.format(cal.getTime());
        cell.setCellValue(dateAndUser);

        // Prompt for data enterer name
        row = instructionsSheet.createRow(rowIndex);
        rowIndex++;
        cell = row.createCell(0);
        cell.setCellStyle(titleStyle);
        cell.setCellValue("Person(s) responsible for data entry [                       ]");

        // Insert additional row before next content
        rowIndex++;

        // Default sheet instructions
        row = instructionsSheet.createRow(rowIndex);
        rowIndex++;
        cell = row.createCell(0);
        cell.setCellStyle(headingStyle);
        cell.setCellValue(defaultSheetName + " Tab");

        row = instructionsSheet.createRow(rowIndex);
        rowIndex++;
        rowIndex++;
        cell = row.createCell(0);
        cell.setCellStyle(wrapStyle);
        cell.setCellValue("Please fill out each field in the \"" + defaultSheetName + "\" tab as completely as possible. " +
                "Fields in red are required (data cannot be uploaded to the database without these fields). " +
                "Required and recommended fields are usually placed towards the beginning of the template. " +
                "Some fields have a controlled vocabulary associated with them in the \"" + listsSheetName + "\" tab " +
                "and are provided as data validation in the provided cells" +
                "If you have more than one entry to a field (i.e. a list of publications), " +
                "please delimit your list with pipes (|).  Also please make sure that there are no newline " +
                "characters (=carriage returns) in any of your metadata. Fields in the " + defaultSheetName + " tab may be re-arranged " +
                "in any order so long as you don't change the field names.");

        // data Fields sheet
        row = instructionsSheet.createRow(rowIndex);
        rowIndex++;
        cell = row.createCell(0);
        cell.setCellStyle(headingStyle);
        cell.setCellValue(dataFieldsSheetName + " Tab");

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
        cell.setCellValue(listsSheetName + " Tab");

        row = instructionsSheet.createRow(rowIndex);
        cell = row.createCell(0);
        cell.setCellStyle(wrapStyle);
        cell.setCellValue("This tab contains controlled vocabulary lists for certain fields.  DO NOT EDIT this sheet!");


        // Create a Box to Hold The Critical Information
        /*
        HSSFPatriarch patriarch = instructionsSheet.createDrawingPatriarch();
        HSSFClientAnchor a = new HSSFClientAnchor(0, 0, 1023, 255, (short) 1, 0, (short) 1, 0);
        HSSFSimpleShape shape1 = patriarch.createSimpleShape(a);
        shape1.setShapeType(HSSFSimpleShape.OBJECT_TYPE_LINE);

        // Create the textbox
        HSSFTextbox textbox = patriarch.createTextbox(
                new HSSFClientAnchor(0, 0, 0, 0, (short) 0, 3, (short) 1, 12));
        textbox.setHorizontalAlignment(CellStyle.ALIGN_CENTER);

        // Accession ID
        HSSFFont font = workbook.createFont();
        font.setColor(IndexedColors.RED.getIndex());
        font.setBoldweight(Font.BOLDWEIGHT_BOLD);
        font.setFontHeightInPoints((short) 18);
        HSSFRichTextString accessionString = new HSSFRichTextString(accessionNumber.toString());
        accessionString.applyFont(font);
        textbox.setString(accessionString);

        //HSSFRichTextString accessionString2 = new HSSFRichTextString("foodad");
        //accessionString.applyFont(2, 5, font);

        textbox.
        //textbox.setString(accessionString2);
        */

    }

    /**
     * Create the Excel File for output.
     * This function ALWAYS  creates XLSX files as this format is the only one
     * which will pass compatibility checks in data validation
     * <p/>
     * Dataset code is used as the basis for the outputfile name
     * If Dataset code is not available, it uses the metadata shortname for project
     * If that is not available it uses "output"
     *
     * @param defaultSheetname
     * @param uploadPath
     * @param fields
     *
     * @return
     */
    public File createExcelFile(String defaultSheetname, String uploadPath, List<String> fields) {

        // Create each of the sheets
        createInstructions(defaultSheetname);
        createDefaultSheet(defaultSheetname, fields);
        createDataFields(fields);
        createListsSheetAndValidations(fields);

        // Create the output Filename and Write Excel File
        String filename = null;
        if (this.datasetCode != null && !this.datasetCode.equals("")) {
            filename = this.datasetCode;
        } else if (mapping.getMetadata().getShortname() != null && !mapping.getMetadata().getShortname().equals("")) {
            filename = mapping.getMetadata().getShortname().replace(" ", "_");
        } else {
            filename = "output";
        }

        // Create the file: NOTE: this application ALWAYS should create XLSX files as this format is the only one
        // which will pass compatibility checks in data validation
        File file = PathManager.createUniqueFile(filename + ".xlsx", uploadPath);
        try {
            FileOutputStream out = new FileOutputStream(file);
            workbook.write(out);
            out.close();
        } catch (IOException e) {
            throw new FimsRuntimeException(500, e);
        }

        return file;
    }

    /**
     * Create an Excel File for output using a pre-uploaded workbook containing a worksheet with data
     * This method assumes the appropriate constructor was called.
     *
     * @param defaultSheetName
     * @param uploadPath
     *
     * @return
     */
    public File createExcelFileFromExistingSources(String defaultSheetName, String uploadPath) {

        // Create each of the sheets
        createInstructions(defaultSheetName);

        // Set the defaultSheet to be the default sheet of the workbook
        defaultSheet = this.workbook.getSheet(defaultSheetName);

        // Getting list of field names...
        // I'm not sure if i should get ALL from mapping file or just the ones specified on the spreadsheet template
        // This method fetches all from mapping file
        XSSFRow row = workbook.getSheet(defaultSheetName).getRow(0);
        ArrayList<String> fields = new ArrayList<String>();
        Iterator it = row.iterator();
        while (it.hasNext()) {
            String fieldName = ((Cell) it.next()).toString();
            // TODO: test implications of adding or NOT adding BCID column at this point
            if (!fieldName.equalsIgnoreCase("BCID")) {
                fields.add(fieldName);
            }
        }

        createDataFields(fields);
        createListsSheetAndValidations(fields);

        // Create the output Filename and Write Excel File
        String filename = null;
        if (this.datasetCode != null && !this.datasetCode.equals("")) {
            filename = this.datasetCode;
        } else if (mapping.getMetadata().getShortname() != null && !mapping.getMetadata().getShortname().equals("")) {
            filename = mapping.getMetadata().getShortname().replace(" ", "_");
        } else {
            filename = "output";
        }

        // Create the file: NOTE: this application ALWAYS should create XLSX files as this format is the only one
        // which will pass compatibility checks in data validation
        File file = PathManager.createUniqueFile(filename + ".xlsx", uploadPath);
        try {
            FileOutputStream out = new FileOutputStream(file);
            workbook.write(out);
            out.close();
        } catch (IOException e) {
            throw new FimsRuntimeException(500, e);
        }

        return file;
    }

    /**
     * Print the abstract text
     *
     * @return
     */
    public String printAbstract() {
        return mapping.getMetadata().getTextAbstract();
    }

    /**
     * Format a key/value string to use in Instructions Sheet Header
     *
     * @param key
     * @param value
     *
     * @return
     */
    private XSSFRichTextString formatKeyValueString(String key, String value) {
        XSSFFont font = workbook.createFont();
        font.setColor(IndexedColors.RED.getIndex());
        font.setBoldweight(Font.BOLDWEIGHT_BOLD);
        font.setFontHeightInPoints((short) 14);
        //String identifier = "Accession Number: ";
        XSSFRichTextString totalRichTextString = new XSSFRichTextString(key + value);
        Integer start = key.toString().length();
        Integer end = totalRichTextString.toString().length();
        // Only make the value portion of string RED
        //totalRichTextString.applyFont(start, end, font);
        // Just make the whole string RED
        totalRichTextString.applyFont(0, end, font);
        return totalRichTextString;
    }

    /**
     * main method is used for testing
     *
     * @param args
     */
    public static void main(String[] args) {
        // File configFile = new ConfigurationFileFetcher(1, "tripleOutput", false).getOutputFile();
        //File file = new File("/Users/jdeck/IdeaProjects/biocode-fims/web_nmnh/docs/SIENT.xml");
        File file = new File("/Users/jdeck/Downloads/ucjeps_fims.xml");
        //TemplateProcessor t1 = new TemplateProcessor(file,"tripleOutput",false,12345,"DEMO4","ark:/99999/fk2");

        //System.out.println(t1.definition("hdimNumber"));


        /*
        TemplateProcessor t = new TemplateProcessor(file, "tripleOutput", false, 12345, "DEMO4", "ark:/21547/VR2");
        ArrayList<String> a = new ArrayList<String>();
               a.add("Locality");
               a.add("ScientificName");
               a.add("Coll_Num");

               File outputFile = t.createExcelFile("Samples", "tripleOutput", a);
               System.out.println(outputFile.getAbsoluteFile().toString());
        */


    }

}
