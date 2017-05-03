package biocode.fims.digester;

import biocode.fims.fimsExceptions.FimsRuntimeException;
import biocode.fims.fimsExceptions.ServerErrorException;
import biocode.fims.reader.plugins.TabularDataReader;
import biocode.fims.renderers.RowMessage;
import biocode.fims.settings.FimsPrinter;
import biocode.fims.utils.DateUtils;
import biocode.fims.utils.EncodeURIcomponent;
import biocode.fims.utils.RegEx;
import biocode.fims.utils.SqlLiteNameCleaner;
import com.fasterxml.jackson.annotation.JsonIgnore;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.validator.routines.UrlValidator;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sqlite.Function;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.*;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

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

    private Mapping mapping;
    public Rule() {}

    public Rule(Mapping mapping) {
        this.mapping = mapping;
    }

    // hack until we get rid of digester. passing this in as a constructor argument was causing dependency conflicts
    // with asm libs
    public void setMapping(Mapping mapping) {this.mapping = mapping;}

    // A reference to the worksheet object this rule belongs to
    // TODO: Remove the remaining references to worksheet.  We are transitioning to using a SQLlite Database connection
    private TabularDataReader worksheet;
    private Worksheet digesterWorksheet;
    // Now a reference to a SQLLite connection
    private java.sql.Connection connection;

    // Rules can also own their own fields
    private final LinkedList<String> fields = new LinkedList<String>();

    private static Logger logger = LoggerFactory.getLogger(Rule.class);

    // NOTE: not sure i want these values in this class, maybe define a sub-class?
    private String decimalLatitude;
    private String decimalLongitude;
    private String maxErrorInMeters;
    private String horizontalDatum;

    private String plateName;
    private String wellNumber;

    private LinkedList<RowMessage> messages = new LinkedList<RowMessage>();

    @JsonIgnore
    public LinkedList<RowMessage> getMessages() {
        return messages;
    }

    @JsonIgnore
    public Worksheet getDigesterWorksheet() {
        return digesterWorksheet;
    }

    public void setDigesterWorksheet(Worksheet digesterWorksheet) {
        this.digesterWorksheet = digesterWorksheet;
    }

    public void setConnection(java.sql.Connection connection) {
        this.connection = connection;
    }

    @JsonIgnore
    public TabularDataReader getWorksheet() {
        return worksheet;
    }

    public void setWorksheet(TabularDataReader worksheet) {
        this.worksheet = worksheet;
        // Synchronize the Excel Worksheet instance with the digester worksheet instance
        //fimsPrinter.out.println("setting to "+ digesterWorksheet.getSheetname());
        worksheet.setTable(digesterWorksheet.getSheetname());
    }

    @JsonIgnore
    public String getDecimalLatitude() {
        return decimalLatitude;
    }

    public void setDecimalLatitude(String decimalLatitude) {
        this.decimalLatitude = decimalLatitude;
    }

    @JsonIgnore
    public String getDecimalLongitude() {
        return decimalLongitude;
    }

    public void setDecimalLongitude(String decimalLongitude) {
        this.decimalLongitude = decimalLongitude;
    }

    @JsonIgnore
    public String getMaxErrorInMeters() {
        return maxErrorInMeters;
    }

    public void setMaxErrorInMeters(String maxErrorInMeters) {
        this.maxErrorInMeters = maxErrorInMeters;
    }

    @JsonIgnore
    public String getHorizontalDatum() {
        return horizontalDatum;
    }

    public void setHorizontalDatum(String horizontalDatum) {
        this.horizontalDatum = horizontalDatum;
    }

    @JsonIgnore
    public String getPlateName() {
        return plateName;
    }

    public void setPlateName(String plateName) {
        this.plateName = plateName;
    }

    @JsonIgnore
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
     * Returns the name of the columnn as it appears to the worksheet
     *
     * @return
     */
    public String getColumn() {
        return column;
    }

    /**
     * Returns the name of the column as it appears to SQLLite
     *
     * @return
     */
    @JsonIgnore
    public String getCleanedColumn() {
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

    @JsonIgnore
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
     * If a dataformat other then "string" is specified for an {@link Attribute}, we check that the data is of the
     * correct type ("integer", "date", etc)
     */
    public void validDataTypeFormat() {
        addSqliteRegExp();
        for (Attribute a : mapping.getAllAttributes(digesterWorksheet.getSheetname())) {
            // we don't need to check if a.getDatatype == null, as this should be checked in the ConfigurationFileTester
            switch (a.getDatatype()) {
                case INTEGER:
                    isIntegerDataFormat(a);
                    break;
                case FLOAT:
                    break;
                case DATE:
                case TIME:
                case DATETIME:
                    // we don't need to check if a.dataformt == null, as this should be checked in the ConfigurationFileTester
                    isDateDataFormat(a);
                    break;
                default:
                    break;
            }
        }
    }

    private void isIntegerDataFormat(Attribute a) {
        Statement statement = null;
        ResultSet rs = null;
        String groupMessage = "Invalid DataFormat";
        String cleanedColumn = new SqlLiteNameCleaner().fixNames(a.getColumn());

        try {
            String sql = "SELECT `" + cleanedColumn + "` FROM `" + digesterWorksheet.getSheetname() + "` WHERE `" + cleanedColumn + "` NOT REGEXP '^\\s*[+-]?\\d*\\s*$'";
            statement = connection.createStatement();

            rs = statement.executeQuery(sql);

            // Hold values that are not good
            ArrayList<String> inValidValues = new ArrayList<>();
            // Loop results
            while (rs.next()) {
                inValidValues.add(rs.getString(cleanedColumn));
            }

            if (inValidValues.size() > 0) {
                addMessage("\"" + a.getColumn() + "\" contains non Integer values: " +
                                StringUtils.join(inValidValues, ", "),
                        groupMessage);
            }
        } catch (SQLException e) {
            // do nothing as the spreadsheet may not contain every column, thus we can get a SQLException
            // complaining about the table not existing
            logger.debug("SQL exception processing isIntegerDataFormat rule" + e.getLocalizedMessage());
            //e.printStackTrace();
        } finally {
            closeDb(statement, rs);
        }
    }

    private void isFloatDataFormat(Attribute a) {
        Statement statement = null;
        ResultSet rs = null;
        String groupMessage = "Invalid DataFormat";
        String cleanedColumn = new SqlLiteNameCleaner().fixNames(a.getColumn());

        try {
            addSqliteRegExp();
            String sql = "SELECT `" + cleanedColumn + "` FROM `" + digesterWorksheet.getSheetname() + "` WHERE `" + cleanedColumn + "` NOT REGEXP '^\\s*[+-]?\\d*\\.*\\d*\\s*$'";
            statement = connection.createStatement();

            rs = statement.executeQuery(sql);

            // Hold values that are not good
            ArrayList<String> inValidValues = new ArrayList<>();
            // Loop results
            while (rs.next()) {
                inValidValues.add(rs.getString(cleanedColumn));
            }

            if (inValidValues.size() > 0) {
                addMessage("\"" + a.getColumn() + "\" contains non Float values: " +
                                StringUtils.join(inValidValues, ", "),
                        groupMessage);
            }
        } catch (SQLException e) {
            // do nothing as the spreadsheet may not contain every column, thus we can get a SQLException
            // complaining about the table not existing
            logger.debug("SQL exception processing isFloatDataFormat rule");
            e.printStackTrace();
        } finally {
            closeDb(statement, rs);
        }
    }

    private void isDateDataFormat(Attribute a) {
        Statement statement = null;
        ResultSet rs = null;
        String groupMessage = "Invalid DataFormat";
        String cleanedColumn = new SqlLiteNameCleaner().fixNames(a.getColumn());

        try {
            String sql = "SELECT `" + cleanedColumn + "` FROM `" + digesterWorksheet.getSheetname() + "`";
            statement = connection.createStatement();

            rs = statement.executeQuery(sql);

            // Hold values that are not good
            ArrayList<String> inValidValues = new ArrayList<>();
            // if the Excel cell is a DateCell, then ExcelReader will parse it as joda-time value.
            // therefore we need to add this format
            String jodaFormat;
            switch (a.getDatatype()) {
                case DATE:
                    jodaFormat = DateUtils.ISO_8061_DATE;
                    break;
                case TIME:
                    jodaFormat = DateUtils.ISO_8061_TIME;
                    break;
                default:
                    jodaFormat = DateUtils.ISO_8061_DATETIME;
                    break;
            }
            String[] formats = (String[]) ArrayUtils.add(a.getDataformat().split(","), jodaFormat);

            while (rs.next()) {
                String value = rs.getString(cleanedColumn);
                if (!StringUtils.isBlank(value) && !biocode.fims.utils.DateUtils.isValidDateFormat(value, formats)) {
                    inValidValues.add(value);
                }
            }

            if (inValidValues.size() > 0) {
                addMessage("\"" + a.getColumn() + "\" contains invalid date values. Format must be an Excel " + a.getDatatype().name() + " or one of [" + a.getDataformat() + "]: " +
                                StringUtils.join(inValidValues, ", "),
                        groupMessage);
            }
        } catch (SQLException e) {
            // do nothing as the spreadsheet may not contain every column, thus we can get a SQLException
            // complaining about the table not existing
            logger.debug("SQL exception processing isDateDataFormat rule \n");
            e.printStackTrace();
        } finally {
            closeDb(statement, rs);
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
            sql = "select `" + getCleanedColumn() + "` from " + digesterWorksheet.getSheetname() +
                    " WHERE ifnull(`" + getCleanedColumn() + "`,'') != '' " +
                    " group by `" + getCleanedColumn() + "`";

            rs = statement.executeQuery(sql);

            // Hold values that are not good
            StringBuilder values = new StringBuilder();
            // Loop results
            while (rs.next()) {
                String value = rs.getString(getCleanedColumn());
                // Compare the list of values of against their encoded counterparts...
                if (!value.equals(encodeURIcomponent.encode(value))) {
                    if (!values.toString().trim().equals("")) {
                        values.append(", ");
                    }
                    values.append(rs.getString(getCleanedColumn()));
                }
            }
            if (!values.toString().trim().equals("")) {
                addMessage("\"" + getColumn() + "\" contains some bad characters: " + values.toString(), groupMessage);
            }


        } catch (SQLException e) {
            logger.debug("SQL exception processing validForURI rule");
            e.printStackTrace();
        } finally {
            closeDb(statement, rs);
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
            sql = "select `" + getCleanedColumn() + "`,count(*) from " + digesterWorksheet.getSheetname() +
                    " WHERE ifnull(`" + getCleanedColumn() + "`,'') != '' " +
                    " group by `" + getCleanedColumn() + "`" +
                    " having count(*) > 1";

            rs = statement.executeQuery(sql);

            // Loop results
            while (rs.next()) {

                if (!values.toString().trim().equals("")) {
                    values.append(", ");
                }
                values.append(rs.getString(getCleanedColumn()));

            }
            if (!values.toString().trim().equals("")) {
                addMessage("\"" + getColumn() + "\" column is defined as unique but some values used more than once: " + values.toString(), groupMessage);
            }

        } catch (SQLException e) {
            logger.debug(null, e);
        } finally {
            closeDb(statement, rs);
        }
    }

    /**
     * Check a particular group of columns to see if all the value combinations are unique.
     * <p>
     * Example:
     * <br></br>
     * {@code
     * <rule type='compositeUniqueValue' level='error'>
     * <field>eventId</field>
     * <field>photoId</field>
     * </rule>
     * }
     */
    public void compositeUniqueValue() {
        String groupMessage = "Unique value constraint did not pass";
        Statement statement = null;
        ResultSet rs = null;

        // clean the field names before working with them
        List<String> cleanFields = fields.stream()
                .map(SqlLiteNameCleaner::fixNames)
                .collect(Collectors.toList());

        try {
            statement = connection.createStatement();
            // Search for non-unique values in resultSet

            String fieldString = String.join(", ", cleanFields);

            String sql = "select " + fieldString + ",count(*) as c from " + digesterWorksheet.getSheetname() +
                    " group by " + fieldString +
                    " having c > 1";

            rs = statement.executeQuery(sql);

            // Loop results
            while (rs.next()) {

                StringBuilder values = new StringBuilder();

                values.append("(");

                for (String field : cleanFields) {
                    String val = rs.getString(field);
                    String key = "";

                    for (String f : fields) {
                        if (field.equals(SqlLiteNameCleaner.fixNames(f))) {
                            key = f;
                            break;
                        }
                    }

                    values.append("\"");
                    values.append(key);
                    values.append("\": \"");
                    values.append(val);
                    values.append("\", ");
                }

                values.deleteCharAt(values.length() - 1); // remove last space
                values.deleteCharAt(values.length() - 1); // remove last ,

                values.append(")");

                addMessage("(\"" + String.join("\", \"", fields) + "\") is defined as a composite unique key, but some value combinations were used more than once: " + values.toString(), groupMessage);
            }

        } catch (SQLException e) {
            logger.debug(null, e);
        } finally {
            closeDb(statement, rs);
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
                    msg = getDecimalLatitude() + " " + latValue + " outside of \"" + getColumn() + "\" bounding box.";
                    addMessage(msg, groupMessage, j);
                }
                if (longValue != null && longValue != 0.0 && (longValue < minLng || longValue > maxLng)) {
                    msg = getDecimalLongitude() + " " + longValue + " outside of \"" + getColumn() + "\" bounding box.";
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
        String minimum = getCleanedColumn().split(",")[0];
        String maximum = getCleanedColumn().split(",")[1];
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
                String sql = "select `" + thisColumn + "` from  " + digesterWorksheet.getSheetname() +
                        " where abs(`" + thisColumn + "`) == 0 AND " +
                        "trim(`" + thisColumn + "`) != '0' AND " +
                        "`" + thisColumn + "` != \"\";";
                resultSet = statement.executeQuery(sql);
                while (resultSet.next()) {
                    msg = "non-numeric value " + resultSet.getString(thisColumn) + " for " + thisColumn;
                    addMessage(msg, groupMessage);
                }
            }

            // Check to see that minimum is less than maximum
            String sql = "select `" + minimum + "`,`" + maximum + "` from " + digesterWorksheet.getSheetname() +
                    " where abs(`" + minimum + "`) > abs(`" + maximum + "`)";
            resultSet = statement.executeQuery(sql);
            while (resultSet.next()) {
                msg = "Illegal values! " + minimum + " = " +
                        resultSet.getString(minimum) +
                        " while " + maximum + " = " +
                        resultSet.getString(maximum);
                addMessage(msg, groupMessage);
            }

        } catch (SQLException e) {
            logger.debug("minimumMaximumCheck exception", e);
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
            // do nothing, just make it not caseInsensitive
        }


        // First check that this column exists before running this rule
        Boolean columnExists = checkColumnExists(getCleanedColumn());
        if (!columnExists) {
            // No need to return a message here if column does not exist
            return;
        }

        // Convert XML Field values to a Stringified list
        listFields = getListElements();
        //listFields = getFields();
        // Loop the fields and put in a StringBuilder
        for (Field listField : listFields) {
            try {
                String value = listField.getValue();
                // NOTE: the following escapes single quotes using another single quote
                // (two single quotes in a row allows us to query one single quote in SQLlite)
                if (caseInsensitiveSearch) {
                    fieldListSB.append("\'" + value.toUpperCase().replace("'", "''") + "\'");
                } else {
                    fieldListSB.append("\'" + value.toString().replace("'", "''") + "\'");
                }
                fieldListArrayList.add(listField.toString());
            } catch (Exception e) {
                logger.debug(null, e);
                // do nothing
            }
        }

        // Query the SQLlite instance to see if these values are contained in a particular row
        try {
            statement = connection.createStatement();
            // Do the select on values based on other column values
            String sql = "SELECT rowid,`" + getCleanedColumn() + "`,`" + getOtherColumn() + "` FROM " + digesterWorksheet.getSheetname();
            sql += " WHERE ifnull(`" + getCleanedColumn() + "`,'') == '' ";

            // if null lookup look that we just have SOME value
            if (fieldListSB.toString().equals("")) {
                sql += " AND ifnull(`" + getOtherColumn() + "`,'') != ''";
                // else we look in the lookup list
            } else {
                sql += " AND `" + getOtherColumn() + "`";
                sql += " IN (" + fieldListSB.toString() + ")";
            }
            //System.out.println(sql);
            resultSet = statement.executeQuery(sql);
            while (resultSet.next()) {
                String column = resultSet.getString(getCleanedColumn()).trim();
                String otherColumn = resultSet.getString(getOtherColumn()).trim();
                int rowNum = resultSet.getInt("rowid");

                // Only display messages for items that exist, that is empty cell contents are an approved value
                if (column.equals("")) {
                    //msg = "\"" + resultSet.getString(getCleanedColumn()) + "\" not an approved " + getColumn() + ", see list";

                    //msg = "\"" + getColumn() + "\" column contains a value, but associated column \"" +
                    //        getOtherColumnWorksheetName() + "\" must be one of: " + listToString(fieldListArrayList);

                    //msg = "\"" + getOtherColumnWorksheetName() + "\" is declared as " + listToString(fieldListArrayList) +
                    msg = "\"" + getOtherColumnWorksheetName() + "\" has value " + "\"" + otherColumn + "\"" +
                            ", but associated column \"" + getColumn() + "\" has no value";
                    //kind of object is declared as 'VALUE' and required Column is empty

                    /* msg += " without an approved value in \"" + getOtherColumnWorksheetName() + "\"";
                    if (!fieldListSB.toString().equals("")) {
                        msg += " (Appropriate \"" + getOtherColumnWorksheetName() + "\" values: {" + fieldListSB.toString() + "})";
                    }*/
                    addMessage(msg, "Dependent column value check", rowNum);
                }
            }

        } catch (SQLException e) {
            logger.debug(null, e);
        } finally {
            try {
                if (statement != null)
                    statement.close();
                if (resultSet != null)
                    resultSet.close();
            } catch (SQLException e) {
                logger.debug("SQLException", e);
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
                    } catch (Exception e) {
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
        String thisColumn = getCleanedColumn();
        String msg = null;
        String sql = "";
        try {
            Statement statement = connection.createStatement();

            // Split the value according to our convention
            String[] values = value.split("=|and");

            // Construct sql for
            sql = "SELECT `" + thisColumn + "`" +
                    " FROM " + digesterWorksheet.getSheetname() +
                    // next line tests whether or not the value is a number
                    " WHERE ( NOT abs(`" + thisColumn + "`) > 0 AND `" + thisColumn + "` != \"0\"" +
                    " OR ( NOT cast(`" + thisColumn + "` as real) " + URLDecoder.decode(values[0], "utf-8");

            if (values.length > 1) {
                sql += " OR NOT cast(`" + thisColumn + "` as real) " + URLDecoder.decode(values[1], "utf-8");
            }

            sql += ")) AND `" + thisColumn + "` != \"\";";
            resultSet = statement.executeQuery(sql);
            while (resultSet.next()) {
                msg = "Value out of range " + resultSet.getString(thisColumn) + " for \"" + getColumn() + "\" using range validation = " + URLDecoder.decode(value, "utf-8");
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
        boolean validNumber = checkValidNumberSQL(getCleanedColumn());
    }

    /**
     * Method that uses SQL to check for valid numbers.   This uses absolute value function in SQLLite
     * and recognizes the following as non-numeric values ("abc","15a","z159")
     * However, the following are recognized as numeric values ("15%", "100$", "1.02E10")
     *
     * @param thisColumn
     * @return
     */
    private boolean checkValidNumberSQL(String thisColumn) {
        String groupMessage = "Invalid number";
        boolean validNumber = true;
        ResultSet resultSet;
        String msg;

        try {
            Statement statement = connection.createStatement();
            String sql = "select `" + thisColumn + "` from  " + digesterWorksheet.getSheetname() +
                    " where abs(`" + thisColumn + "`) == 0 AND " +
                    "trim(`" + thisColumn + "`) != '0' AND " +
                    "`" + thisColumn + "` != \"\";";
            resultSet = statement.executeQuery(sql);
            while (resultSet.next()) {
                msg = "Non-numeric value " + resultSet.getString(thisColumn) + " for \"" + getColumn() + "\"";
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
     * @return
     */
    private boolean checkValidNumber(String rowValue) {
        if (rowValue != null && !rowValue.equals("")) {

            if (rowValue.indexOf(".") > 0) {
                try {
                    Double.parseDouble(rowValue);
                } catch (NumberFormatException nme) {
                    return false;
                }
            } else {
                try {
                    Integer.parseInt(rowValue);
                } catch (NumberFormatException nme) {
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
            sql = "SELECT `" + getDecimalLatitude() + "`,`" + getDecimalLongitude() + "`" +
                    " FROM " + digesterWorksheet.getSheetname() +
                    " WHERE " +
                    " abs(`" + getDecimalLatitude() + "`) " + URLDecoder.decode(">=90", "utf-8") +
                    " OR abs(`" + getDecimalLatitude() + "`) " + URLDecoder.decode("<=-90", "utf-8") +
                    " OR abs(`" + getDecimalLongitude() + "`) " + URLDecoder.decode(">=180", "utf-8") +
                    " OR abs(`" + getDecimalLongitude() + "`) " + URLDecoder.decode("<=-180", "utf-8") +
                    " AND `" + getDecimalLatitude() + "` != \"\"" +
                    " AND `" + getDecimalLongitude() + "` != \"\"";


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
     * Duplicate of controlledVocabulary
     */
    @Deprecated
    public void checkInXMLFields() {
        controlledVocabulary();
    }

    /**
     * controlledVocabulary specifies lookup list values.  There are two ways of referring to lookup, lists:
     * <p></p>
     * <p/>
     * 1. Listing values in XML fields, like: <br></br>
     * {@code
     * <rule type="controlledVocabulary" name="PreparationType" level="warning">
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
     * <rule type="controlledVocabulary" name="relaxant" level="warning" list="list3"/>
     * }
     */
    public void controlledVocabulary() {
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
            // do nothing, just make it not caseInsensitive
        }

        // First check that this column exists before running this rule
        Boolean columnExists = checkColumnExists(getCleanedColumn());
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
                logger.debug("Exception", e);
                // do nothing
            }
        }
        // Query the SQLlite instance to see if these values are contained in a particular row


        try {
            statement = connection.createStatement();
            String sql = "select rowid,`" + getCleanedColumn() + "` from " + digesterWorksheet.getSheetname() +
                    " where (`" + getCleanedColumn() + "` NOT NULL AND `" + getCleanedColumn() + "` != \"\") AND ";
            if (caseInsensitiveSearch)
                sql += "UPPER(`" + getCleanedColumn() + "`)";
            else
                sql += "`" + getCleanedColumn() + "`";
            sql += " not in (" + lookupSB.toString() + ")";
            resultSet = statement.executeQuery(sql);
            while (resultSet.next()) {
                String value = resultSet.getString(getCleanedColumn()).trim();
                int rowNum = resultSet.getInt("rowid");
                // Only display messages for items that exist or na, that is empty cell contents are an approved value
                if (!value.equals("")) {

                    msg = "\"" + resultSet.getString(getCleanedColumn()) + "\" not an approved \"" + getColumn() + "\"";

                    String groupMessage = "\"" + getColumn() + "\" contains invalid value <a  href=\"#\" onclick=\"list('" + getList() +
                            "','" + column + "');\">see list</a>";

                    addMessage(msg, groupMessage, rowNum);
                }
            }

        } catch (SQLException e) {
            throw new FimsRuntimeException("SQL exception processing checkInXMLFields rule", 500, e);
        } finally {
            closeDb(statement, resultSet);
        }
    }

    /**
     * RequiredColumn looks for a single required columns in spreadsheet
     * <p></p>
     * Example:
     * <br></br>
     * {@code
     * <rule type="RequiredColumn" name="fieldName" level="error">
     * </rule>
     * }
     */
    public void RequiredColumn() {
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

            booFound = false;

            // fieldNameWorksheet has spaces
            fieldNameWorksheet = getCleanedColumn();
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

            if (notFoundArray.size() > 0) {
                msg = "Did not find " + levelValue + " columns: " + listToString(notFoundArray);
                addMessage(msg, groupMessage);
            }
        } catch (SQLException e) {
            throw new FimsRuntimeException(500, e);
        } finally {
            closeDb(statement, rs);
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
            closeDb(statement, rs);
        }
    }

    /**
     * Convert an ArrayList to a string
     *
     * @param list
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
    private List<Field> getListElements() {
        List<Field> fields = new ArrayList<>();
        Validation v = digesterWorksheet.getValidation();
        if (v != null) {
            biocode.fims.digester.List list = v.findList(getList());
            if (list != null) {
                fields.addAll(v.findList(getList()).getFields());
            }
        }
        return fields;
    }

    /**
     * Add a message with just a message and no row assigned
     *
     * @param message
     */
    private void addMessage
    (String message, String groupMessage) {
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
    private Integer getMessageLevel() {
        if (this.getLevel().equals("warning"))
            return RowMessage.WARNING;
        else
            return RowMessage.ERROR;
    }

    /**
     * A simple check to see if a column exists in the SQLLite Database
     *
     * @param column
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
            logger.debug(null, e);
        }
    }

    /**
     * get ruleMetadata
     *
     * @param sList We pass in a List of fields we want to associate with this rule
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
                logger.debug("UnsupportedEncodingException", e);
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

    /**
     * datasetContainsExtraColumns check to see if the uploaded dataset contains any columns that
     * do not exist in the project configuration
     */
    public void datasetContainsExtraColumns() {
        String groupMessge = "Undefined columns. Column(s) will not be persisted.";
        List<String> datasetColumns = worksheet.getColNames();
        List<String> definedColumns = mapping.getColumnNames();

        for (String colName : datasetColumns) {
            if (!definedColumns.contains(colName)) {
                addMessage("Dataset contains undefined column: " + colName, groupMessge);
            }
        }
    }

    /**
     * isValidUrl checks to see if a string is a valid Url, with the schemes {"http", "https"}
     */
    public void isValidUrl() {
        String[] schemes = {"http", "https"};
        UrlValidator urlValidator = new UrlValidator(schemes);
        String groupMessage = "Invalid URL";
        String thisColumn = getCleanedColumn();
        ResultSet resultSet;
        String msg;

        try {
            Statement statement = connection.createStatement();
            String sql = "SELECT `" + thisColumn + "` FROM " + digesterWorksheet.getSheetname() + ";";
            resultSet = statement.executeQuery(sql);
            while (resultSet.next()) {
                String value = resultSet.getString(thisColumn);
                if (!StringUtils.isBlank(value) && !urlValidator.isValid(value)) {
                    msg = resultSet.getString(thisColumn) + " is not a valid URL for \"" + getColumn() + "\"";
                    addMessage(msg, groupMessage);
                }
            }
        } catch (SQLException e) {
            throw new ServerErrorException(e);
        }
    }

    /**
     * rule to check that at least 1 column in a group of columns has a value
     * <p>
     * Example:
     * <p>
     * <rule type="requiredColumnInGroup" level="error">
     * <field>column1</field>
     * <field>column2</field>
     * </rule>
     */
    public void requiredColumnInGroup() {
        String groupMessage = "Missing column from group";
        Statement statement = null;
        ResultSet rs = null;
        ResultSet rsColumns = null;
        try {
            statement = connection.createStatement();
            rsColumns = statement.executeQuery("select * from " + digesterWorksheet.getSheetname() + " limit 0");
            ResultSetMetaData rsMetadata = rsColumns.getMetaData();

            List<String> columnsInWorksheet = new ArrayList<>();
            // check that the fields exist in the worksheet sqlite table
            for (int i = 1; i <= rsMetadata.getColumnCount(); i++) {
                String column = rsMetadata.getColumnName(i);
                if (fields.contains(column)) {
                    columnsInWorksheet.add(column);
                }
            }

            if (columnsInWorksheet.size() == 0) {
                addMessage("at least 1 column(s) " + Arrays.toString(fields.toArray()) + " is required.", groupMessage);
            } else {
                StringBuilder sql = new StringBuilder();
                sql.append("select rowId from ");
                sql.append(digesterWorksheet.getSheetname());
                sql.append(" where ");

                int cnt = 1;
                for (String field : fields) {
                    sql.append("ifnull(");
                    sql.append(SqlLiteNameCleaner.fixNames(field));
                    sql.append(", '') = '' ");

                    if (cnt++ < fields.size()) {
                        sql.append("and ");
                    }
                }

                rs = statement.executeQuery(sql.toString());
                while (rs.next()) {
                    int rowNum = rs.getInt("rowId");
                    addMessage("at least 1 cell value in the column group " + Arrays.toString(fields.toArray()) + " is required.", groupMessage, rowNum);
                }
            }
        } catch (SQLException e) {
            throw new FimsRuntimeException(500, e);
        } finally {
            closeDb(statement, rs);
            closeDb(null, rsColumns);
        }
    }

    private void closeDb(Statement statement, ResultSet rs) {
        try {
            if (statement != null)
                statement.close();
            if (rs != null)
                rs.close();
        } catch (SQLException e) {
            logger.debug(null, e);
        }
    }

    /**
     * adds a REGEXP function to sqlite
     *
     * @throws SQLException
     */
    private void addSqliteRegExp() {
        try {
            Function.create(connection, "REGEXP", new Function() {
                @Override
                protected void xFunc() throws SQLException {
                    String expression = value_text(0);
                    String value = value_text(1);
                    if (value == null)
                        value = "";

                    Pattern pattern = Pattern.compile(expression);
                    result(pattern.matcher(value).find() ? 1 : 0);
                }
            });
        } catch (SQLException e) {
            // do nothing. Most likely the function was already created
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Rule)) return false;

        Rule rule = (Rule) o;

        if (getLevel() != null ? !getLevel().equals(rule.getLevel()) : rule.getLevel() != null) return false;
        if (getType() != null ? !getType().equals(rule.getType()) : rule.getType() != null) return false;
        if (getColumn() != null ? !getColumn().equals(rule.getColumn()) : rule.getColumn() != null) return false;
        if (getList() != null ? !getList().equals(rule.getList()) : rule.getList() != null) return false;
        if (getValue() != null ? !getValue().equals(rule.getValue()) : rule.getValue() != null) return false;
        if (getOtherColumn() != null ? !getOtherColumn().equals(rule.getOtherColumn()) : rule.getOtherColumn() != null)
            return false;
        return getFields() != null ? getFields().equals(rule.getFields()) : rule.getFields() == null;
    }

    @Override
    public int hashCode() {
        int result = getLevel() != null ? getLevel().hashCode() : 0;
        result = 31 * result + (getType() != null ? getType().hashCode() : 0);
        result = 31 * result + (getColumn() != null ? getColumn().hashCode() : 0);
        result = 31 * result + (getList() != null ? getList().hashCode() : 0);
        result = 31 * result + (getValue() != null ? getValue().hashCode() : 0);
        result = 31 * result + (getOtherColumn() != null ? getOtherColumn().hashCode() : 0);
        result = 31 * result + (getFields() != null ? getFields().hashCode() : 0);
        return result;
    }
}
