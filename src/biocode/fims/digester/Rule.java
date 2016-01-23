package biocode.fims.digester;

import biocode.fims.fimsExceptions.FimsException;
import biocode.fims.fimsExceptions.FimsRuntimeException;
import biocode.fims.reader.plugins.TabularDataReader;
import biocode.fims.renderers.RowMessage;
import biocode.fims.settings.FimsPrinter;
import biocode.fims.utils.EncodeURIcomponent;
import biocode.fims.utils.RegEx;
import biocode.fims.utils.SqlLiteNameCleaner;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.*;
import java.util.List;

/**
 * Rule does the heavy lift for the Validation Components.
 * This is where the code is written for each of the rules
 * encountered in the XML configuration file.
 * <p></p>
 * You can use this javadoc to see a listing of available rules by
 * ignoring the getter and setter methods below and using the method name
 * as the rule "type" in the XML configuration file.  Examples of use, as
 * they appear in the XML configuration file are given below.
 */
public class Rule {

    // General values
    private String level;
    private String type;
    private String column;
    private String list;
    private String value;
    private String otherColumn;

    // A reference to the validation object this rule belongs to
    //private Validation validation;
    // A reference to the worksheet object this rule belongs to
    // TODO: Remove the remaining references to worksheet.  We are transitioning to using a SQLlite Database connection
    private TabularDataReader worksheet;
    private Worksheet digesterWorksheet;
    // Now a reference to a SQLLite connection
    private java.sql.Connection connection;
    // Reference a service Root so we can include service calls where needed
    private String serviceRoot;

    // Rules can also own their own fields
    private final LinkedList<String> fields = new LinkedList<String>();
    //private List fields = new List();

    private static Logger logger = LoggerFactory.getLogger(Rule.class);

    // NOTE: not sure i want these values in this class, maybe define a sub-class?
    // values for DwCLatLngChecker (optional)
    private String decimalLatitude;
    private String decimalLongitude;
    private String maxErrorInMeters;
    private String horizontalDatum;

    private String plateName;
    private String wellNumber;

    private LinkedList<RowMessage> messages = new LinkedList<RowMessage>();

    public LinkedList<RowMessage> getMessages() {
        return messages;
    }

    public Worksheet getDigesterWorksheet() {
        return digesterWorksheet;
    }

    public void setDigesterWorksheet(Worksheet digesterWorksheet) {
        this.digesterWorksheet = digesterWorksheet;
    }

    public void setConnection(java.sql.Connection connection) {
        this.connection = connection;
    }

    public TabularDataReader getWorksheet() {
        return worksheet;
    }

    public void setWorksheet(TabularDataReader worksheet) {
        this.worksheet = worksheet;
        // Synchronize the Excel Worksheet instance with the digester worksheet instance
        //fimsPrinter.out.println("setting to "+ digesterWorksheet.getSheetname());
        try {
            worksheet.setTable(digesterWorksheet.getSheetname());
        } catch (FimsException e) {
            throw new FimsRuntimeException(500, e);
        }
    }

    public void setServiceRoot(String serviceRoot) {
        this.serviceRoot = serviceRoot;
    }

    public String getDecimalLatitude() {
        return decimalLatitude;
    }

    public void setDecimalLatitude(String decimalLatitude) {
        this.decimalLatitude = decimalLatitude;
    }

    public String getDecimalLongitude() {
        return decimalLongitude;
    }

    public void setDecimalLongitude(String decimalLongitude) {
        this.decimalLongitude = decimalLongitude;
    }

    public String getMaxErrorInMeters() {
        return maxErrorInMeters;
    }

    public void setMaxErrorInMeters(String maxErrorInMeters) {
        this.maxErrorInMeters = maxErrorInMeters;
    }

    public String getHorizontalDatum() {
        return horizontalDatum;
    }

    public void setHorizontalDatum(String horizontalDatum) {
        this.horizontalDatum = horizontalDatum;
    }

    public String getPlateName() {
        return plateName;
    }

    public void setPlateName(String plateName) {
        this.plateName = plateName;
    }

    public String getWellNumber() {
        return wellNumber;
    }

    public void setWellNumber(String wellNumber) {
        this.wellNumber = wellNumber;
    }

    public String getList() {
        return list;
    }

    public void setList(String list) {
        this.list = list;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getLevel() {
        return level;
    }

    public void setLevel(String level) {
        this.level = level;
    }

    /**
     * Returns the name of the column as it appears to SQLLite
     *
     * @return
     */
    public String getColumn() {
        // replace spaces with underscores....
        if (column == null) {
            return null;
        } else {
            return new SqlLiteNameCleaner().fixNames(column);
            //return column.replace(" ", "_");
        }
    }

    /**
     * Returns the name of the column as it appears to SQLLite
     *
     * @return
     */
    public String getOtherColumn() {
        // replace spaces with underscores....
        if (otherColumn == null) {
            return null;
        } else {
            return new SqlLiteNameCleaner().fixNames(otherColumn);
            //return column.replace(" ", "_");
        }
    }

    /**
     * Returns the name of the columnn as it appears to the worksheet
     *
     * @return
     */
    public String getColumnWorksheetName() {
        return column;
    }

    public String getOtherColumnWorksheetName() {
        return otherColumn;
    }

    public void setColumn(String column) {
        this.column = column;
    }

    public void setOtherColumn(String otherColumn) {
        this.otherColumn = otherColumn;
    }

    public void addField(String field) {
        fields.add(field);
    }

    /**
     * getFields returns a standard list of not part of a List
     * see getListElement for lookup list Field values
     *
     * @return
     */
    public LinkedList<String> getFields() {
        return fields;
    }

    public void print() {
        //fimsPrinter.out.println("    rule type = " + this.type + "; column = " + this.column + "; level = " + this.level);

        for (Iterator i = fields.iterator(); i.hasNext(); ) {
            String field = (String) i.next();
            FimsPrinter.out.println("      field data : " + field);
        }
    }

    public void run(Object o) {
        //To change body of implemented methods use File | Settings | File Templates.
    }


    /**
     * Warns user if the number of rows the application is looking at differs from what is on the spreadsheet
     */
    public void rowNumMismatch() {
        int bVRows = worksheet.getNumRows();
        int poiRows = worksheet.getSheet().getLastRowNum();
        if (bVRows < poiRows) {
            String theWarning = "bioValidator processed " + bVRows + " rows, but it appears there may be " + poiRows +
                    " rows on your sheet. This usually occurs when cell formatting has extended below the area of data but " +
                    "may also occur when you have left blank rows in the data.";
            messages.addLast(new RowMessage(theWarning, "Spreadsheet check", RowMessage.WARNING));
        }
    }

    /**
     * Check to see if there duplicateColumn Headers on a worksheet
     * <p></p>
     * Example:
     * <br></br>
     * {@code
     * <rule type='duplicateColumnNames' level='error'></rule>
     * }
     */
    public void duplicateColumnNames() {
        List<String> listSheetColumns = worksheet.getColNames();
        Set<String> output = new HashSet<String>();  // Set does not allow duplicates

        for (int i = 0; i < listSheetColumns.size(); i++) {
            String columnValue = worksheet.getStringValue(i, 0);
            for (int j = 0; j < listSheetColumns.size(); j++) {
                if (j != i) {
                    String columnValue2 = worksheet.getStringValue(j, 0);
                    if (columnValue != null && columnValue2 != null) {
                        if (columnValue.equals(columnValue2)) {
                            output.add("row heading " + columnValue + " used more than once");
                            // quite this if we found at least one match
                            j = listSheetColumns.size();
                        }
                    }
                }
            }
        }

        // Loop through output set
        Iterator outputit = output.iterator();
        while (outputit.hasNext()) {
            String message = (String) outputit.next();
            messages.addLast(new RowMessage(message, "Spreadsheet check", RowMessage.WARNING));
        }
    }


    /**
     * Checks that characters in a string can become a portion of a valid URI
     * This is necessary for cases where data is being triplified and constructed as a URI
     * One approach is to encode all characters, however, this creates a mis-leading Bcid
     * and if used as part of a URI should be only valid characters.
     * <p/>
     * Characters that are disallowed are: %$&+,/:;=?@<>#%\
     * <p/>
     * Note that this rule does not check if this a valid URI in its entirety, only that the portion of
     * the string, when appended onto other valid URI syntax, will not break the URI itself
     */
    public void validForURI() {
        String sql = "";
        Statement statement = null;
        ResultSet rs = null;
        String groupMessage = "Non-valid URI characters";
        EncodeURIcomponent encodeURIcomponent = new EncodeURIcomponent();

        // Extract all unique Values from SQL
        try {
            statement = connection.createStatement();
            rs = null;
            // Search for non-unique values in resultSet
            sql = "select " + getColumn() + " from " + digesterWorksheet.getSheetname() +
                    " WHERE ifnull(" + getColumn() + ",'') != '' " +
                    " group by " + getColumn();

            rs = statement.executeQuery(sql);

            // Hold values that are not good
            StringBuilder values = new StringBuilder();
            // Loop results
            while (rs.next()) {
                String value = rs.getString(getColumn());
                // Compare the list of values of against their encoded counterparts...
                if (!value.equals(encodeURIcomponent.encode(value))) {
                    if (!values.toString().trim().equals("")) {
                        values.append(", ");
                    }
                    values.append(rs.getString(getColumn()));
                }
            }
            if (!values.toString().trim().equals("")) {
                addMessage("\"" + getColumnWorksheetName() + "\" contains some bad characters: " + values.toString(), groupMessage);
            }


        } catch (SQLException e) {
            throw new FimsRuntimeException("SQL exception processing uniqueValue rule", 500, e);
        } finally {
            try {
                if (statement != null)
                    statement.close();
                if (rs != null)
                    rs.close();
            } catch (SQLException e) {
                logger.warn(null, e);
            }
        }
    }

    /**
     * Check a particular column to see if all the values are unique.
     * This rule is strongly encouraged for at least
     * one column in the spreadsheet
     * NOTE: that NULL values are not counted in this rule, so this rule, by itself does not
     * enforce a primary key... it must be combined with a rule requiring some column value
     * <p></p>
     * Example:
     * <br></br>
     * {@code
     * <rule type='uniqueValue' column='materialSampleID' level='error'></rule>
     * }
     */
    public void uniqueValue() {
        String groupMessage = "Unique value constraint did not pass";
        String sql = "";
        Statement statement = null;
        ResultSet rs = null;
        StringBuilder values = new StringBuilder();

        try {
            statement = connection.createStatement();
            rs = null;
            // Search for non-unique values in resultSet
            sql = "select " + getColumn() + ",count(*) from " + digesterWorksheet.getSheetname() +
                    " WHERE ifnull(" + getColumn() + ",'') != '' " +
                    " group by " + getColumn() +
                    " having count(*) > 1";

            rs = statement.executeQuery(sql);

            // Loop results
            while (rs.next()) {

                if (!values.toString().trim().equals("")) {
                    values.append(", ");
                }
                values.append(rs.getString(getColumn()));

            }
            if (!values.toString().trim().equals("")) {
                addMessage("\"" + getColumnWorksheetName() + "\" column is defined as unique but some values used more than once: " + values.toString(), groupMessage);
            }

        } catch (SQLException e) {
            throw new FimsRuntimeException("SQL exception processing uniqueValue rule", 500, e);
        } finally {
            try {
                if (statement != null)
                    statement.close();
                if (rs != null)
                    rs.close();
            } catch (SQLException e) {
                logger.warn(null, e);
            }
        }
    }


    /**
     * Check that coordinates are inside a bounding box.
     * The bounding box declaration uses well-known text.
     * <p/>
     * Example:
     * <br></br>
     * {@code
     * <rule type="BoundingBox" name="Moorea" decimalLatitude="DecimalLatitude" decimalLongitude="DecimalLongitude"
     * level="warning">
     * <field>BOX3D(-18.5 -150.8,-16.7 -148.4)</field>
     * </rule>
     * }
     */
    public void BoundingBox() {
        String groupMessage = "Coordinates outside specified bounding box";

        // Build List of XML Fields
        List<String> listFields = getFields();

        // Parse the BOX3D well known text box
        String field = listFields.get(0);
        field = field.replace("BOX3D(", "").replace(")", "");
        String[] points = field.split(",");
        double minLat = Double.parseDouble(points[0].split(" ")[0]);
        double maxLat = Double.parseDouble(points[1].split(" ")[0]);
        double minLng = Double.parseDouble(points[0].split(" ")[1]);
        double maxLng = Double.parseDouble(points[1].split(" ")[1]);

        String msg = "";
        // Loop All Rows in this list and see if they are unique
        for (int j = 1; j <= worksheet.getNumRows(); j++) {
            Double latValue = 0.0;
            Double longValue = 0.0;

            if (!checkValidNumber(worksheet.getStringValue(getDecimalLatitude(), j)) ||
                    !checkValidNumber(worksheet.getStringValue(getDecimalLongitude(), j))) {
                addMessage("Unable to perform BoundingBox check due to illegal Latitude or Longitude value", groupMessage, j);
            } else {

                latValue = worksheet.getDoubleValue(getDecimalLatitude(), j);
                longValue = worksheet.getDoubleValue(getDecimalLongitude(), j);

                if (latValue != null && latValue != 0.0 && (latValue < minLat || latValue > maxLat)) {
                    msg = getDecimalLatitude() + " " + latValue + " outside of \"" + getColumnWorksheetName() + "\" bounding box.";
                    addMessage(msg, groupMessage, j);
                }
                if (longValue != null && longValue != 0.0 && (longValue < minLng || longValue > maxLng)) {
                    msg = getDecimalLongitude() + " " + longValue + " outside of \"" + getColumnWorksheetName() + "\" bounding box.";
                    addMessage(msg, groupMessage, j);
                }
            }
        }
    }


    /**
     * Check that minimum/maximum numbers are entered correctly.
     * <p></p>
     * Example:
     * <br>
     * </br>
     * {@code
     * <rule type='minimumMaximumNumberCheck' column='minimumDepthInMeters,maximumDepthInMeters'
     * level='error'></rule>
     * }
     */
    public void minimumMaximumNumberCheck() {
        String groupMessage = "Number outside of range";
        String minimum = getColumn().split(",")[0];
        String maximum = getColumn().split(",")[1];
        String minMaxArray[] = new String[]{minimum, maximum};

        // Don't run this method if one of these columns doesn't exist
        Boolean minimumExists = checkColumnExists(minimum);
        Boolean maximumExists = checkColumnExists(maximum);
        // No warning message if neither exist
        if (!minimumExists && !maximumExists) {
            // If neither minimum or maximum exist then just ignore this
            // messages.addLast(new RowMessage(
            //         "Unable to run minimumMaximumNumberCheck rule since Neither " + minimum + " or " + maximum + " columns exist",
            //        RowMessage.WARNING));
            return;
        } else if (!minimumExists && maximumExists) {
            messages.addLast(new RowMessage("Column " + maximum + " exists but must have corresponding column " + minimum, "Spreadsheet check", RowMessage.WARNING));
            return;
        } else if (minimumExists && !maximumExists) {
            messages.addLast(new RowMessage("Column " + minimum + " exists but must have corresponding column " + maximum, "Spreadsheet check", RowMessage.WARNING));
            return;
        }

        Statement statement = null;
        ResultSet resultSet = null;
        String msg;
        try {
            statement = connection.createStatement();

            // Look for non numeric values in minimum & maximum columns
            for (String thisColumn : Arrays.asList(minMaxArray)) {
                String sql = "select " + thisColumn + " from  " + digesterWorksheet.getSheetname() +
                        " where abs(" + thisColumn + ") == 0 AND " +
                        "trim(" + thisColumn + ") != '0' AND " +
                        thisColumn + " != \"\";";
                resultSet = statement.executeQuery(sql);
                while (resultSet.next()) {
                    msg = "non-numeric value " + resultSet.getString(thisColumn) + " for " + thisColumn;
                    addMessage(msg, groupMessage);
                }
            }

            // Check to see that minimum is less than maximum
            String sql = "select " + minimum + "," + maximum + " from " + digesterWorksheet.getSheetname() +
                    " where abs(" + minimum + ") > abs(" + maximum + ")";
            resultSet = statement.executeQuery(sql);
            while (resultSet.next()) {
                msg = "Illegal values! " + minimum + " = " +
                        resultSet.getString(minimum) +
                        " while " + maximum + " = " +
                        resultSet.getString(maximum);
                addMessage(msg, groupMessage);
            }

        } catch (SQLException e) {
            throw new FimsRuntimeException("minimumMaximumCheck exception", 500, e);
        }
    }

    /**
     * Check that lowestTaxonLevel and LowestTaxon are entered correctly (Biocode Database only)
     */
    public void checkLowestTaxonLevel() {
        String groupMessage = "Lowest taxon level not entered correctly";

        for (int j = 1; j <= worksheet.getNumRows(); j++) {
            String LowestTaxonLevelValue = worksheet.getStringValue("LowestTaxonLevel", j);
            String LowestTaxonValue = worksheet.getStringValue("LowestTaxon", j);

            if (LowestTaxonLevelValue == null && LowestTaxonValue != null) {
                addMessage("LowestTaxon entered without a LowestTaxonLevel", groupMessage, j);
            } else if (LowestTaxonLevelValue != null && LowestTaxonValue == null) {
                addMessage("LowestTaxonLevel entered without a LowestTaxon", groupMessage, j);
            }
        }
    }

    /**
     * Return the index of particular columns
     *
     * @param columns
     *
     * @return
     */
    protected int[] getColumnIndices(String[] columns) {
        int[] b = new int[columns.length];
        List<String> listSheetColumns = worksheet.getColNames();
        for (int col = 0; col < columns.length; col++) {
            b[col] = -1;
            for (int cname = 0; cname < listSheetColumns.size(); cname++) {
                String heading = worksheet.getStringValue(cname, 0).trim();
                if (columns[col].equals(heading)) {
                    b[col] = cname;
                }
            }
        }
        return b;
    }


    /**
     * SI created this function to validate genus & species counts.
     * Collectors SHOULD collect exactly 4 samples for each Genus species
     *
     * @throws Exception
     */
    @Deprecated
    void checkGenusSpeciesCountsSI() throws Exception {
        String groupMessage = "Check Genus and species counts for SI";
        String genusHeading = "Genus";
        String speciesHeading = "Species";
        String[] headings = {genusHeading, speciesHeading};
        int[] columnIndices = this.getColumnIndices(headings);
        //Collectors SHOULD collect exactly 4 samples for each Genus species
        //This is just intended as a warning.
        int genusIndex = columnIndices[0];
        int speciesIndex = columnIndices[1];

        if (genusIndex == -1 || speciesIndex == -1) {
            addMessage("Did not find Genus / species column headings in spreadsheet", groupMessage, 0);
            return;
        }

        Hashtable<String, Integer> genusSpeciesCombos = new Hashtable<String, Integer>();
        for (int row = 1; row <= worksheet.getNumRows(); row++) {
            String genusSpecies = worksheet.getStringValue(genusIndex, row).trim() + " " +
                    worksheet.getStringValue(speciesIndex, row).trim();
            Integer gsCount = genusSpeciesCombos.get(genusSpecies);
            if (gsCount == null) {
                gsCount = 0;
            }
            gsCount += 1;
            genusSpeciesCombos.put(genusSpecies, gsCount);
        }

        Set<String> set = genusSpeciesCombos.keySet();

        Iterator<String> itr = set.iterator();

        String key;
        while (itr.hasNext()) {
            key = itr.next();
            Integer count = genusSpeciesCombos.get(key);
            if (count > 4) {
                addMessage("You collected " + count + " " + key + ". Should collect 4.", groupMessage, 0);
            } else if (count < 4) {
                addMessage("You collected " + count + " " + key + ". Should collect at least 4.", groupMessage, 0);
            }
        }
    }


    /**
     * Smithsonian created rule to check Voucher heading
     *
     * @param worksheet
     *
     * @throws Exception
     */
    @Deprecated
    public void checkVoucherSI(TabularDataReader worksheet) throws Exception {
        String groupMessage = "Check voucher SI rule";
        String[] headings = {"Voucher Specimen?", "Herbarium Accession No./Catalog No.", "Herbarium Acronym"};
        int[] columnIndices = this.getColumnIndices(headings);
        int vsIdx = columnIndices[0];
        int hanIdx = columnIndices[1];
        int haIdx = columnIndices[2];

        if (vsIdx == -1) {
            addMessage("Did not find Voucher Specimen heading in spreadsheet.", groupMessage, 0);
        }

        if (hanIdx == -1) {
            addMessage("Did  not find Herbarium Accession No./Catalog No. column heading in spreadsheet.", groupMessage, 0);
        }

        if (haIdx == -1) {
            addMessage("Did not find Herbarium Acronym heading in spreadsheet.", groupMessage, 0);
        }

        if (vsIdx == -1 || hanIdx == -1 || haIdx == -1) {
            return;
        }

        for (int row = 1; row <= worksheet.getNumRows(); row++) {
            String voucher = worksheet.getStringValue(vsIdx, row);
            if (voucher == null) {
                addMessage("Missing value for 'Voucher Specimen?'. Must be Y or N.", groupMessage, row);
                continue;
            }
            voucher = voucher.trim();
            if (voucher.equals("Y")) {
                String han = worksheet.getStringValue(hanIdx, row);
                if (han == null) {
                    addMessage("Missing Herbarium Accession No./Catalog No. for voucher specimen.", groupMessage, row);
                } else if (han.trim().length() <= 2) {
                    addMessage("Herbarium Accession No./Catalog No. must be at least two characters long.", groupMessage, row);
                }

                String ha = worksheet.getStringValue(haIdx, row);
                if (ha == null) {
                    addMessage("Missing Herbarium Acronym for voucher specimen.", groupMessage, row);
                } else if (ha.trim().length() == 0) {
                    addMessage("Herbarium Acronym must be at least one character long.", groupMessage, row);
                }

            }
        }
    }

    /**
     * If a user enters data in a particular column, it is required to:
     * 1.  have a value in second column
     * 2.  if there is a list of values specified under the rule, it needs to match one of those values
     */
    public void requiredValueFromOtherColumn() {

        StringBuilder fieldListSB = new StringBuilder();
        ArrayList<String> fieldListArrayList = new ArrayList<String>();
        List<Field> listFields = null;
        String msg;
        ResultSet resultSet = null;
        Statement statement = null;

        Boolean caseInsensitiveSearch = false;
        try {
            if (digesterWorksheet.getValidation().findList(getList()).getCaseInsensitive().equalsIgnoreCase("true")) {
                caseInsensitiveSearch = true;
            }
        } catch (NullPointerException e) {
            logger.warn("NullPointerException", e);
            // do nothing, just make it not caseInsensitive
        }

        // First check that this column exists before running this rule
        Boolean columnExists = checkColumnExists(getColumn());
        if (!columnExists) {
            // No need to return a message here if column does not exist
            //messages.addLast(new RowMessage("Column name " + getColumn() + " does not exist", RowMessage.WARNING));
            return;
        }

        // Convert XML Field values to a Stringified list
        listFields = getListElements();
        //listFields = getFields();
        // Loop the fields and put in a StringBuilder
        int count = 0;
        for (int k = 0; k < listFields.size(); k++) {
            try {
                String value = ((Field) listFields.get(k)).getValue();
                // if (count > 0)
                //     lookupSB.append(",");
                // NOTE: the following escapes single quotes using another single quote
                // (two single quotes in a row allows us to query one single quote in SQLlite)
                if (caseInsensitiveSearch) {
                    fieldListSB.append("\'" + value.toUpperCase().replace("'", "''") + "\'");
                } else {
                    fieldListSB.append("\'" + value.toString().replace("'", "''") + "\'");
                }
                fieldListArrayList.add(listFields.get(k).toString());

                count++;
            } catch (Exception e) {
                //TODO should we be catching Exception?
                logger.warn("Exception", e);
                // do nothing
            }
        }

        // Query the SQLlite instance to see if these values are contained in a particular row
        try {
            statement = connection.createStatement();
            // Do the select on values based on other column values
            String sql = "SELECT rowid," + getColumn() + "," + getOtherColumn() + " FROM " + digesterWorksheet.getSheetname();
            sql += " WHERE ifnull(" + getColumn() + ",'') == '' ";

            // if null lookup look that we just have SOME value
            if (fieldListSB.toString().equals("")) {
                sql += " AND ifnull(" + getOtherColumn() + ",'') != ''";
                // else we look in the lookup list
            } else {
                sql += " AND " + getOtherColumn();
                sql += " IN (" + fieldListSB.toString() + ")";
            }
            //System.out.println(sql);
            resultSet = statement.executeQuery(sql);
            while (resultSet.next()) {
                String column = resultSet.getString(getColumn()).trim();
                String otherColumn = resultSet.getString(getOtherColumn()).trim();
                int rowNum = resultSet.getInt("rowid");

                // Only display messages for items that exist, that is empty cell contents are an approved value
                if (column.equals("")) {
                    //msg = "\"" + resultSet.getString(getColumn()) + "\" not an approved " + getColumn() + ", see list";

                    //msg = "\"" + getColumnWorksheetName() + "\" column contains a value, but associated column \"" +
                    //        getOtherColumnWorksheetName() + "\" must be one of: " + listToString(fieldListArrayList);

                    //msg = "\"" + getOtherColumnWorksheetName() + "\" is declared as " + listToString(fieldListArrayList) +
                    msg = "\"" + getOtherColumnWorksheetName() + "\" has value " + "\"" + otherColumn + "\"" +
                            ", but associated column \"" + getColumnWorksheetName() + "\" has no value";
                    //kind of object is declared as 'VALUE' and required Column is empty

                    /* msg += " without an approved value in \"" + getOtherColumnWorksheetName() + "\"";
                    if (!fieldListSB.toString().equals("")) {
                        msg += " (Appropriate \"" + getOtherColumnWorksheetName() + "\" values: {" + fieldListSB.toString() + "})";
                    }*/
                    addMessage(msg, "Dependent column value check", rowNum);
                }
            }

        } catch (SQLException e) {
            throw new FimsRuntimeException("SQL exception processing requiredValueFromOtherColumn rule", 500, e);
        } finally {
            try {
                if (statement != null)
                    statement.close();
                if (resultSet != null)
                    resultSet.close();
            } catch (SQLException e) {
                logger.warn("SQLException", e);
            }
        }
    }

    /**
     * Smithsonian created rule.
     * No more than 96 rows per plate No.
     * Row letter between A-H
     * Column No between 01-12
     * No duplicate plate_row_column
     */
    @Deprecated
    public void checkTissueColumnsSI() throws Exception {
        String groupMessage = "Check tissue columns SI rule";
        String plateNoHeading = "Plate No.";
        String rowLetterHeading = "Row Letter";
        String columnNumberHeading = "Column No.";
        int plateNoIndex = -1;
        int rowLetterIndex = -1;
        int colNoIndex = -1;


        List<String> listSheetColumns = worksheet.getColNames();
        for (int col = 0; col < listSheetColumns.size(); col++) {
            String columnValue = worksheet.getStringValue(col, 0);
            if (columnValue.equals(plateNoHeading)) {
                plateNoIndex = col;
            } else if (columnValue.equals(rowLetterHeading)) {
                rowLetterIndex = col;
            } else if (columnValue.equals(columnNumberHeading)) {
                colNoIndex = col;
            }
        }

        //This check may be redundant...
        if (colNoIndex == -1 || rowLetterIndex == -1 || plateNoIndex == -1) {
            addMessage("Did not find required headings for Plate / Well Row / Well Column in spreadsheet", groupMessage, 0);
            return;
        }

        //Check no more than 96 rows per plate no. using a hashtable of plate numbers for the counters.
        Hashtable<String, Integer> plateCounts = new Hashtable<String, Integer>();
        //Check we have no duplicate Plate + Row + Column combinations
        Hashtable<String, Integer> plateRowColumnCombined = new Hashtable<String, Integer>();
        for (int row = 1; row <= worksheet.getNumRows(); row++) {
            //Get column values
            String plateNo = worksheet.getStringValue(plateNoIndex, row).trim();
            Integer plateCount = plateCounts.get(plateNo);
            if (plateCount == null) {
                plateCount = 0; //This is the initializer
            }
            plateCount += 1;

            if (plateCount > 96) {
                addMessage("Too many rows for plate " + plateNo, groupMessage, row);
            }
            plateCounts.put(plateNo, plateCount);

            //Row letter must be A-H only (no lowercase)
            String rowLetter = worksheet.getStringValue(rowLetterIndex, row).trim();
            if (!RegEx.verifyValue("^[A-H]$", rowLetter)) {
                addMessage("Bad row letter " + rowLetter + " (must be between A and H)", groupMessage, row);
            }

            //Column Number must be 01-12
            String colNo = worksheet.getStringValue(colNoIndex, row).trim();
            int col;
            try {
                col = Integer.parseInt(colNo);
                if (col < 1 && col > 12) {
                    addMessage("Bad Column Number " + colNo + " (must be between 01 and 12).", groupMessage, row);
                }
            } catch (NumberFormatException e) {
                logger.warn("NumberFormatException", e);
                addMessage("Invalid number format for Column Number", groupMessage, row);
            }

            String prc = plateNo + rowLetter + colNo;
            Integer prcRow = plateRowColumnCombined.get(prc);
            if (prcRow != null) {
                addMessage("Duplicate Plate / Row / Column combination (previously defined at row " + prcRow + ")", groupMessage, row);
            } else {
                plateRowColumnCombined.put(prc, row);
            }
        }
    }


    /**
     * Check that well number and plate name are entered together and correctly.  Takes a tissue and plate column
     * specificaiton
     * and sees that well numbers are formatted well and with range.
     * <p></p>
     * Example:
     * <br>
     * </br>
     * {@code
     * <rule type="checkTissueColumns" name="" plateName="format_name96" wellNumber="well_number96" level="warning"/>
     * }
     */
    public void checkTissueColumns() {
        String groupMessage = "Well number / plate names not entered correctly";

        for (int j = 1; j <= worksheet.getNumRows(); j++) {

            String format_name96Value = worksheet.getStringValue(getPlateName(), j);
            String well_number96Value = worksheet.getStringValue(getWellNumber(), j);

            if (format_name96Value == null && well_number96Value != null) {
                addMessage("Well Number (well_number96) entered without a Plate Name (format_name96)", groupMessage, j);
            } else if (format_name96Value != null && well_number96Value == null) {
                addMessage("Plate Name (format_name96) entered without a Well Number (well_number96)", groupMessage, j);
            } else if (format_name96Value == null && well_number96Value == null) {
                // ignore case where both are null (just means no tissue entered)
            } else {
                if (RegEx.verifyValue("(^[A-Ha-h])(\\d+)$", well_number96Value)) {
                    Integer intNumber = null;

                    try {
                        String strNumber = well_number96Value.substring(1, well_number96Value.length());
                        intNumber = Integer.parseInt(strNumber);
                    } catch (NumberFormatException nme) {
                        logger.warn("NumberFormatException", nme);
                        // Not a valid integer
                        addMessage("Bad Well Number " + well_number96Value, groupMessage, j);
                    } catch (Exception e) {
                        //TODO should we be catching Exception?
                        logger.warn("Exception", e);
                        addMessage("Bad Well Number " + well_number96Value, groupMessage, j);
                    } finally {
                        if (intNumber <= 12 && intNumber >= 1) {
                            // ok
                        } else {
                            // Number OK but is out of range
                            addMessage("Bad Well Number " + well_number96Value, groupMessage, j);
                        }
                    }
                } else {
                    // Something bigger wrong with well number syntax
                    addMessage("Bad Well Number " + well_number96Value, groupMessage, j);
                }
            }

        }
    }

    /**
     * Checks for valid numeric values, looking in the value attribute field for ranges.
     * Multiple ranges can be specified in value, like:   value:">=-90 and <=90"
     * or, simply value:">=0"
     */
    public void validateNumeric() {
        String groupMessage = "Invalid number format";
        boolean validNumber = true;
        ResultSet resultSet;
        String thisColumn = getColumn();
        String msg = null;
        String sql = "";
        try {
            Statement statement = connection.createStatement();

            // Split the value according to our convention
            String[] values = value.split("=|and");

            // Construct sql for
            sql = "SELECT " + thisColumn +
                    " FROM " + digesterWorksheet.getSheetname() +
                    " WHERE " +
                    "   abs(" + thisColumn + ") " + URLDecoder.decode(values[0], "utf-8");

            if (values.length > 1) {
                sql += " and abs(" + thisColumn + ") " + URLDecoder.decode(values[1], "utf-8");
            }

            sql += " and " + thisColumn + " != \"\";";
            resultSet = statement.executeQuery(sql);
            while (resultSet.next()) {
                msg = "Value out of range " + resultSet.getString(thisColumn) + " for \"" + getColumnWorksheetName() + "\" using range validation = " + value;
                addMessage(msg, groupMessage);
                validNumber = false;
            }

        } catch (SQLException e) {
            throw new FimsRuntimeException(500, e);
        } catch (UnsupportedEncodingException e) {
            throw new FimsRuntimeException(500, e);
        }
    }

    /**
     * checks for valid numbers.
     * {@code
     * This uses absolute value function in SQLLite
     * and recognizes the following as non-numeric values ("abc","15a","z159")
     * However, the following are recognized as numeric values ("15%", "100$", "1.02E10")
     * }
     */
    public void isNumber() {
        boolean validNumber = checkValidNumberSQL(getColumn());
    }

    /**
     * Method that uses SQL to check for valid numbers.   This uses absolute value function in SQLLite
     * and recognizes the following as non-numeric values ("abc","15a","z159")
     * However, the following are recognized as numeric values ("15%", "100$", "1.02E10")
     *
     * @param thisColumn
     *
     * @return
     */
    private boolean checkValidNumberSQL(String thisColumn) {
        String groupMessage = "Invalid number";
        boolean validNumber = true;
        ResultSet resultSet;
        String msg;

        try {
            Statement statement = connection.createStatement();
            String sql = "select " + thisColumn + " from  " + digesterWorksheet.getSheetname() +
                    " where abs(" + thisColumn + ") == 0 AND " +
                    "trim(" + thisColumn + ") != '0' AND " +
                    thisColumn + " != \"\";";
            resultSet = statement.executeQuery(sql);
            while (resultSet.next()) {
                msg = "Non-numeric value " + resultSet.getString(thisColumn) + " for \"" + getColumnWorksheetName() + "\"";
                addMessage(msg, groupMessage);
                validNumber = false;
            }
        } catch (SQLException e) {
            throw new FimsRuntimeException(500, e);
        }
        return validNumber;
    }

    /**
     * Check that this is a valid Number, for internal use only
     *
     * @param rowValue
     *
     * @return
     */
    private boolean checkValidNumber(String rowValue) {
        if (rowValue != null && !rowValue.equals("")) {

            if (rowValue.indexOf(".") > 0) {
                try {
                    Double.parseDouble(rowValue);
                } catch (NumberFormatException nme) {
                    logger.warn("NumberFormatException", nme);
                    return false;
                }
            } else {
                try {
                    Integer.parseInt(rowValue);
                } catch (NumberFormatException nme) {
                    logger.warn("NumberFormatException", nme);
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * Checks for valid lat/lng values and warns about maxerrorinmeters and horizontaldatum values.
     * <p></p>
     * Example:
     * <br></br>
     * {@code
     * <rule
     * type="DwCLatLngChecker"
     * decimalLatitude="DecimalLatitude"
     * decimalLongitude="DecimalLongitude"
     * maxErrorInMeters="MaxErrorInMeters"
     * horizontalDatum="HorizontalDatum"
     * level="warning"/>
     * }
     */
    public void DwCLatLngChecker() {
        String msg = "";
        String groupMessage = "Invalid Latitude / longitude";
        for (int j = 1; j <= worksheet.getNumRows(); j++) {
            Double latValue = null;
            Double lngValue = null;

            latValue = worksheet.getDoubleValue(getDecimalLatitude(), j);
            lngValue = worksheet.getDoubleValue(getDecimalLongitude(), j);

            String datumValue = worksheet.getStringValue(getHorizontalDatum(), j);
            String maxerrorValue = worksheet.getStringValue(getMaxErrorInMeters(), j);

            if (!checkValidNumber(worksheet.getStringValue(getDecimalLatitude(), j))) {
                addMessage(worksheet.getStringValue(getDecimalLatitude(), j) + " not a valid " + getDecimalLatitude(), groupMessage, j);
            }
            if (!checkValidNumber(worksheet.getStringValue(getDecimalLongitude(), j))) {
                addMessage(worksheet.getStringValue(getDecimalLongitude(), j) + " not a valid " + getDecimalLongitude(), groupMessage, j);
            }

            if (lngValue != null && latValue == null) {
                msg = getDecimalLongitude() + " entered without a " + getDecimalLatitude();
                addMessage(msg, groupMessage, j);
            }

            if (lngValue == null && latValue != null) {
                msg = getDecimalLatitude() + " entered without a " + getDecimalLongitude();
                addMessage(msg, groupMessage, j);
            }

            if (datumValue != null && (lngValue == null && latValue == null)) {
                msg = getHorizontalDatum() + " entered without a " + getDecimalLatitude() + " or " + getDecimalLongitude();
                addMessage(msg, groupMessage, j);
            }

            if (maxerrorValue != null && (lngValue == null && latValue == null)) {
                msg = getMaxErrorInMeters() + " entered without a " + getDecimalLatitude() + " or " + getDecimalLongitude();
                addMessage(msg, groupMessage, j);
            }

        }
    }

    /**
     * Checks for valid lat/lng values
     * <p></p>
     * Example:
     * <br></br>
     * {@code
     * <rule
     * type="latLngChecker"
     * decimalLatitude="DecimalLatitude"
     * decimalLongitude="DecimalLongitude"
     * level="warning"/>
     * }
     */
    public void latLngChecker() {
        String msg = "";
        String groupMessage = "Invalid latitude / longitude";
        String sql = null;
        ResultSet rs = null;

        try {
            Statement statement = connection.createStatement();

            // Construct sql for
            sql = "SELECT " + getDecimalLatitude() + "," + getDecimalLongitude() +
                    " FROM " + digesterWorksheet.getSheetname() +
                    " WHERE " +
                    " abs(" + getDecimalLatitude() + ") " + URLDecoder.decode("<=90", "utf-8") +
                    " AND abs(" + getDecimalLatitude() + ") " + URLDecoder.decode(">=-90", "utf-8") +
                    " AND abs(" + getDecimalLongitude() + ") " + URLDecoder.decode("<=180", "utf-8") +
                    " AND abs(" + getDecimalLongitude() + ") " + URLDecoder.decode(">=-180", "utf-8") +
                    " AND " + getDecimalLatitude() + " != \"\"" +
                    " AND " + getDecimalLongitude() + " != \"\"";


            rs = statement.executeQuery(sql);
            while (rs.next()) {
                msg = "Bad Latitude/Longitude Value " + rs.getString(getDecimalLatitude()) + "/" + rs.getString(getDecimalLongitude());
                addMessage(msg, groupMessage);
            }

        } catch (SQLException e) {
            throw new FimsRuntimeException(500, e);
        } catch (UnsupportedEncodingException e) {
            throw new FimsRuntimeException(500, e);
        }
    }

    /**
     * Duplicate of checkInXMLFields
     */
    public void controlledVocabulary() {
        checkInXMLFields();
    }

    /**
     * checkInXMLFields specifies lookup list values.  There are two ways of referring to lookup, lists:
     * <p></p>
     * <p/>
     * 1. Listing values in XML fields, like: <br></br>
     * {@code
     * <rule type="checkInXMLFields" name="PreparationType" level="warning">
     * <field>card_mount</field>
     * <field>envelope</field>
     * <field>fluid</field>
     * <field>pin</field>
     * <field>riker_mount</field>
     * <field>slide</field>
     * <field>resin_mount</field>
     * <field>pin_genitalia_vial</field>
     * <field>other</field>
     * </rule>
     * }
     * <p></p>
     * <p/>
     * 2. Refering to values in a lookup list in the configuration file list element, like: <br></br>
     * {@code
     * <rule type="checkInXMLFields" name="relaxant" level="warning" list="list3"/>
     * }
     */
    public void checkInXMLFields() {
        StringBuilder lookupSB = new StringBuilder();
        List<Field> listFields = null;
        String msg;
        ResultSet resultSet = null;
        Statement statement = null;

        Boolean caseInsensitiveSearch = false;
        try {
            if (digesterWorksheet.getValidation().findList(getList()).getCaseInsensitive().equalsIgnoreCase("true")) {
                caseInsensitiveSearch = true;
            }
        } catch (NullPointerException e) {
            logger.warn("NullPointerException", e);
            // do nothing, just make it not caseInsensitive
        }

        // First check that this column exists before running this rule
        Boolean columnExists = checkColumnExists(getColumn());
        if (!columnExists) {
            // No need to return a message here if column does not exist
            //messages.addLast(new RowMessage("Column name " + getColumn() + " does not exist", RowMessage.WARNING));
            return;
        }

        // Convert XML Field values to a Stringified list
        listFields = getListElements();
        // Loop the fields and put in a StringBuilder
        int count = 0;
        for (int k = 0; k < listFields.size(); k++) {
            try {
                String value = ((Field) listFields.get(k)).getValue();
                if (count > 0)
                    lookupSB.append(",");
                // NOTE: the following escapes single quotes using another single quote
                // (two single quotes in a row allows us to query one single quote in SQLlite)
                if (caseInsensitiveSearch)
                    lookupSB.append("\'" + value.toUpperCase().replace("'", "''") + "\'");
                else
                    lookupSB.append("\'" + value.replace("'", "''") + "\'");
                count++;
            } catch (Exception e) {
                //TODO should we be catching Exception?
                logger.warn("Exception", e);
                // do nothing
            }
        }
        // Query the SQLlite instance to see if these values are contained in a particular row


        try {
            statement = connection.createStatement();
            String sql = "select rowid," + getColumn() + " from " + digesterWorksheet.getSheetname() +
                    " where (" + getColumn() + " NOT NULL AND " + getColumn() + " != \"\") AND ";
            if (caseInsensitiveSearch)
                sql += "UPPER(" + getColumn() + ")";
            else
                sql += getColumn();
            sql += " not in (" + lookupSB.toString() + ")";
            resultSet = statement.executeQuery(sql);
            while (resultSet.next()) {
                String value = resultSet.getString(getColumn()).trim();
                int rowNum = resultSet.getInt("rowid");
                // Only display messages for items that exist or na, that is empty cell contents are an approved value
                if (!value.equals("")) {

                    msg = "\"" + resultSet.getString(getColumn()) + "\" not an approved \"" + getColumnWorksheetName() + "\"";

                    String groupMessage = "\"" + getColumnWorksheetName() + "\" contains invalid value <a  href=\"#\" onclick=\"list('" + getList() +
                            "','" + column + "');\">see list</a>";

                    addMessage(msg, groupMessage, rowNum);
                }
            }

        } catch (SQLException e) {
            throw new FimsRuntimeException("SQL exception processing checkInXMLFields rule", 500, e);
        } finally {
            try {
                if (statement != null)
                    statement.close();
                if (resultSet != null)
                    resultSet.close();
            } catch (SQLException e) {
                logger.warn("SQLException", e);
            }
        }
    }

    /**
     * RequiredColumns looks for required columns in spreadsheet by looking for them in the <field> tags
     * <p></p>
     * Example:
     * <br></br>
     * {@code
     * <rule type="RequiredColumns" name="RequiredColumns" level="error">
     * <field>Coll_EventID_collector</field>
     * <field>EnteredBy</field>
     * <field>Specimen_Num_Collector</field>
     * <field>HoldingInstitution</field>
     * <field>Phylum</field>
     * </rule>
     * }
     */
    public void RequiredColumns() {
        String groupMessage = "Missing column(s)";
        Statement statement = null;
        ResultSet rs = null;
        try {
            statement = connection.createStatement();

            // Set text for this warning values
            String levelValue = "mandatory";
            if (getMessageLevel() == RowMessage.WARNING) {
                levelValue = "desirable";
            }

            String fieldNameSQLLite = "", msg = "", fieldNameWorksheet = "";
            ArrayList<String> notFoundArray = new ArrayList<String>();
            SqlLiteNameCleaner cleaner = new SqlLiteNameCleaner();
            boolean booFound = false;
            // Create a hashset of column names for easy lookup
            Set<String> hashset = new HashSet<String>(worksheet.getColNames());
            // Loop through the list of required fields using the iterator
            Iterator itRequiredField = getFields().iterator();
            while (itRequiredField.hasNext()) {
                booFound = false;

                // fieldNameWorksheet has spaces
                fieldNameWorksheet = itRequiredField.next().toString().trim();
                // fieldNameSQLLite has underscores instead of spaces
                fieldNameSQLLite = cleaner.fixNames(fieldNameWorksheet);

                // Simple search in hashset for required field name
                if (hashset.contains(fieldNameWorksheet)) {
                    booFound = true;
                }

                // Error message if column not found
                if (!booFound) {
                    notFoundArray.add(fieldNameWorksheet);
                    // Examine column contents -- required columns need some content
                } else {
                    String sql = "";

                    sql = "select count(*) from " + digesterWorksheet.getSheetname() + " where `" + fieldNameSQLLite + "`='' or `" + fieldNameSQLLite + "` is null";
                    rs = statement.executeQuery(sql);
                    if (rs.getInt(1) > 0) {
                        addMessage("\"" + fieldNameWorksheet + "\" has a missing cell value", groupMessage);
                    }
                }
            }

            if (notFoundArray.size() > 0) {
                msg = "Did not find " + levelValue + " columns: " + listToString(notFoundArray);
                addMessage(msg, groupMessage);
            }
        } catch (SQLException e) {
            throw new FimsRuntimeException(500, e);
        } finally {
            try {
                if (statement != null)
                    statement.close();
                if (rs != null)
                    rs.close();
            } catch (SQLException e) {
                logger.warn("SQLException", e);
            }
        }
    }

    /**
     * Convert an ArrayList to a string
     *
     * @param list
     *
     * @return
     */
    private static String listToString(List<?> list) {
        StringBuilder result = new StringBuilder();

        // If only one value then just return that
        if (list.size() == 1) {
            return "\"" + list.get(0).toString() + "\"";
        }

        // If more than one value then return an array syntax
        for (int i = 0; i < list.size(); i++) {
            if (i == 0)
                result.append("[");
            result.append("\"" + list.get(i) + "\"");
            if (i < list.size() - 1)
                result.append(", ");
            if (i == list.size() - 1)
                result.append("]");
        }
        return result.toString();
    }

    /**
     * Get field values associated with a particular list
     *
     * @return
     */
    private List getListElements() {
        Validation v = digesterWorksheet.getValidation();
        if (v == null) {
            return null;
        } else {
            return v.findList(getList()).getFields();
        }
    }

    /**
     * Add a message with a given row and message
     *
     * @param row
     * @param message
     */
    /*private void addMessage
        (Integer row,
        String message) {
        messages.addLast(new RowMessage(message, getMessageLevel(), row));
    }*/

    /**
     * Add a message with just a message and no row assigned
     *
     * @param message
     */
    private void addMessage
    (String message, String groupMessage) {
        messages.addLast(new RowMessage(message, groupMessage, getMessageLevel()));
    }

    public void addMessage(String message) {
        String groupMessage = "Rule doesn't exist or no such Rule";
        messages.addLast(new RowMessage(message, groupMessage, getMessageLevel()));
    }

    private void addMessage
            (String message,
             String groupMessage,
             Integer row) {
        messages.addLast(new RowMessage(message, groupMessage, getMessageLevel(), row));
    }

    /**
     * Get the message level we're working with for this rule
     *
     * @return
     */
    private Integer getMessageLevel
    () {
        if (this.getLevel().equals("warning"))
            return RowMessage.WARNING;
        else
            return RowMessage.ERROR;
    }

    /**
     * A simple check to see if a column exists in the SQLLite Database
     *
     * @param column
     *
     * @return
     */
    private boolean checkColumnExists(String column) {
        try {
            Statement statement = connection.createStatement();
            ResultSet rs = statement.executeQuery("SELECT sql FROM sqlite_master WHERE sql like '%" + column + "%'");

            if (rs.next())
                return true;
            else
                return false;
        } catch (SQLException e) {
            throw new FimsRuntimeException("SQLException checking if " + column + " column exists", 500, e);
        }
    }

    public void close() {
        try {
            connection.close();
        } catch (SQLException e) {
            logger.warn(null, e);
        }
    }

    /**
     * get ruleMetadata
     *
     * @param sList We pass in a List of fields we want to associate with this rule
     *
     * @return
     */
    public JSONObject getRuleMetadata(biocode.fims.digester.List sList) {
        JSONObject ruleMetadata = new JSONObject();
        JSONArray list = new JSONArray();

        ruleMetadata.put("type", this.type);
        // warning level
        ruleMetadata.put("level", this.level);

        if (value != null) {
            try {
                ruleMetadata.put("value", URLDecoder.decode(this.value, "utf-8"));
            } catch (UnsupportedEncodingException e) {
                ruleMetadata.put("value", this.value);
                logger.warn("UnsupportedEncodingException", e);
            }
        }
        // Display fields
        // Convert XML Field values to a Stringified list
        List listFields = null;
        if (sList != null && sList.getFields().size() > 0) {
            listFields = sList.getFields();
        } else {
            listFields = getFields();
        }
        if (listFields != null) {
            Iterator it = listFields.iterator();
            // One or the other types of list need data
            while (it.hasNext()) {
                String field = ((Field) it.next()).getValue();
                list.add(field);
            }
        }
        ruleMetadata.put("list", list);
        return ruleMetadata;
    }

}