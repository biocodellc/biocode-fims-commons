package biocode.fims.digester;

import biocode.fims.fimsExceptions.FimsRuntimeException;
import biocode.fims.reader.SqLiteJsonConverter;
import biocode.fims.reader.plugins.TabularDataReader;
import biocode.fims.renderers.RendererInterface;
import biocode.fims.renderers.RowMessage;
import biocode.fims.settings.FimsPrinter;
import biocode.fims.settings.PathManager;
import org.apache.commons.digester3.Digester;
import org.apache.commons.digester3.ObjectCreateRule;
import org.apache.commons.lang.StringUtils;
import org.json.simple.JSONArray;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;


/**
 * digester.Validation class holds all worksheets that are part of this validator
 */
public class Validation implements RendererInterface {
    // Loop all the worksheets associated with the validation element
    private final LinkedList<Worksheet> worksheets = new LinkedList<Worksheet>();
    // Loop all the lists associated with the validation element
    private final LinkedList<List> lists = new LinkedList<List>();
    // Create a tabularDataReader for reading the data source associated with the validation element
    private TabularDataReader tabularDataReader = null;
    // A SQL Lite connection is mainted by the validation class so we can run through the various rules
    private java.sql.Connection connection = null;
    private static Logger logger = LoggerFactory.getLogger(Validation.class);
    // File reference for a sqlite Database
    private File sqliteFile;
    private SqLiteJsonConverter sdc;

    /**
     * Construct using tabularDataReader object, defining how to read the incoming tabular data
     */
    public Validation() {

    }

    /**
     * The reference to the SQLite instance
     *
     * @return
     */
    public File getSqliteFile() {
        return sqliteFile;
    }

    public TabularDataReader getTabularDataReader() {
        return tabularDataReader;
    }

    /**
     * Add a worksheet to the validation component
     *
     * @param w
     */
    public void addWorksheet(Worksheet w) {
        worksheets.addLast(w);
    }

    public LinkedList<Worksheet> getWorksheets() {
        return worksheets;
    }

    /**
     * Add a list to the validation component
     *
     * @param l
     */
    public void addList(List l) {
        lists.addLast(l);
    }

    /**
     * Get the set of lists defined by this validation object
     *
     * @return
     */
    public LinkedList<List> getLists() {
        return lists;
    }

    /**
     * Lookup a list by its alias
     *
     * @param alias
     *
     * @return
     */
    public List findList(String alias) {
        for (Iterator<List> i = lists.iterator(); i.hasNext(); ) {
            List l = i.next();
            if (l.getAlias().equals(alias))
                return l;
        }
        return null;
    }

    /**
     * Lookup a list for a given column and worksheet
     * @param column
     * @param sheetname
     * @return
     */
    public List findListForColumn(String column, String sheetname) {
        Worksheet sheet = null;
        // Get a list of rules for the first digester.Worksheet instance
        for (Worksheet s: worksheets) {
            if (s.getSheetname().equals(sheetname)) {
                sheet = s;
            }
        }

        if (sheet != null) {
            java.util.List<Rule> rules = sheet.getRules();
            Iterator it = rules.iterator();

            while (it.hasNext()) {
                Rule rule = (Rule) it.next();
                if (rule.getList() != null && StringUtils.equals(rule.getColumn(), column)) {
                    return findList(rule.getList());
                }
            }
        }

        return null;
    }

    /**
     * Create a SQLLite Database instance
     *
     * @return
     */
    public Connection createSqlLite(String filenamePrefix, String outputFolder, String sheetName, JSONArray fimsMetadata) {
        PathManager pm = new PathManager();
        File processDirectory = pm.setDirectory(outputFolder);

        // Load the SQLite JDBC driver.
        try {
            Class.forName("org.sqlite.JDBC");
        } catch (ClassNotFoundException ex) {
            throw new FimsRuntimeException("could not load the SQLite JDBC driver.", 500, ex);
        }

        // Create SQLite file
        String pathPrefix = processDirectory + File.separator + filenamePrefix;
        sqliteFile = PathManager.createUniqueFile(pathPrefix + ".sqlite", outputFolder);

        sdc = new SqLiteJsonConverter(fimsMetadata, "jdbc:sqlite:" + sqliteFile.getAbsolutePath());
        sdc.convert(sheetName);

        // Create the SQLLite connection
        try {
            biocode.fims.settings.Connection localConnection = new biocode.fims.settings.Connection(sqliteFile);
            return java.sql.DriverManager.getConnection(localConnection.getJdbcUrl());
        } catch (SQLException e) {
            throw new FimsRuntimeException("Trouble finding SQLLite Connection", 500, e);
        }

    }

    /**
     * Loop through worksheets and print out object data
     */
    public void printObject() {
        FimsPrinter.out.println("Validate");

        for (Iterator<Worksheet> i = worksheets.iterator(); i.hasNext(); ) {
            Worksheet w = i.next();
            w.print();
        }
    }

    /**
     * Standard print method
     */
    public void print() {

    }

    /**
     * iterate through each Worksheet and append the messages to the processController
     */
    /**
     * iterate through each {@link Worksheet} and collect the messages
     * @return k,v map of sheetName: RowMessage list
     */
    public HashMap<String, LinkedList<RowMessage>> getMessages() {
        HashMap<String, LinkedList<RowMessage>> messages = new HashMap<>();
        for (Iterator<Worksheet> w = worksheets.iterator(); w.hasNext(); ) {
            Worksheet worksheet = w.next();
            messages.put(worksheet.getSheetname(), worksheet.getMessages());
        }
        return messages;
    }

    public boolean hasErrors() {
        for (Worksheet w: worksheets) {
            if (!w.errorFree()) {
                return true;
            }
        }

        return false;
    }

    public boolean hasWarnings() {
        for (Worksheet w: worksheets) {
            if (!w.warningFree()) {
                return true;
            }
        }

        return false;
    }

    /**
     * Begin the validation run.process, looping through worksheets
     *
     * @return
     */
    public boolean run(TabularDataReader tabularDataReader, String filenamePrefix, String outputFolder, Mapping mapping, JSONArray fimsMetadata) {
        this.tabularDataReader = tabularDataReader;

        Worksheet sheet = worksheets.get(0);
        String sheetName = sheet.getSheetname();
        tabularDataReader.setTable(sheetName);

        // Use the errorFree variable to control validation checking workflow
        boolean errorFree = true;

        // Create the SQLLite Connection and catch any exceptions, and send to Error Message
        // Exceptions generated here are most likely useful to the user and the result of SQL exceptions when
        // processing data, such as worksheets containing duplicate column names, which will fail the data load.

        connection = createSqlLite(filenamePrefix, outputFolder, sheetName, fimsMetadata);

        // Attempt to build hashes
        boolean hashErrorFree = true;
        try {
            sdc.buildHashes(mapping, sheetName);
        } catch (Exception e) {
            logger.warn("", e);
            hashErrorFree = false;
        }

        // Run validation components
        boolean processingErrorFree = true;
        if (errorFree) {
            // Loop rules to be run after connection
            for (Iterator<Worksheet> i = worksheets.iterator(); i.hasNext(); ) {
                Worksheet w = i.next();

                boolean thisError = w.run(this);
                if (processingErrorFree) processingErrorFree = thisError;
            }
        }

        if (processingErrorFree && hashErrorFree) {
            return true;
        } else if (!hashErrorFree) {
            sheet.getMessages().addLast(new RowMessage(
                    "Error building hashes.  Likely a required column constraint failed.",
                    "Initial Spreadsheet check",
                    RowMessage.ERROR));
            return false;
        } else {
            return false;
        }
    }

    /**
     * Close the validation component when we're done with it-- here we close the reference to the SQLlite connection
     */
    public void close() {
        try {
            if (connection != null) {
                connection.close();
            }
        } catch (SQLException e) {
            logger.warn("SQLException", e);
        }
    }

    public java.sql.Connection getConnection() {
        return connection;
    }

    /**
     * Process validation component rules
     *
     * @param mapping
     */
    public synchronized void addValidationRules(File configFile, Mapping mapping) {
        Digester d = new Digester();
        d.push(this);

        // Create worksheet objects
        d.addObjectCreate("fims/validation/worksheet", Worksheet.class);
        d.addSetProperties("fims/validation/worksheet");
        d.addSetNext("fims/validation/worksheet", "addWorksheet");

        ObjectCreateRule ruleCreateRule = new ObjectCreateRule(Rule.class);
        ruleCreateRule.setConstructorArgumentTypes(Mapping.class);
        ruleCreateRule.setDefaultConstructorArguments(mapping);

        // Create rule objects
        d.addRule("fims/validation/worksheet/rule", ruleCreateRule);
        d.addSetProperties("fims/validation/worksheet/rule");
        d.addSetNext("fims/validation/worksheet/rule", "addRule");
        d.addCallMethod("fims/validation/worksheet/rule/field", "addField", 0);

        // Create list objects
        d.addObjectCreate("fims/validation/lists/list", List.class);
        d.addSetProperties("fims/validation/lists/list");
        d.addSetNext("fims/validation/lists/list", "addList");
        //d.addCallMethod("fims/validation/lists/list/field", "addField", 0);

        // Create field objects
        d.addObjectCreate("fims/validation/lists/list/field", Field.class);
        d.addSetProperties("fims/validation/lists/list/field");
        d.addSetNext("fims/validation/lists/list/field", "addField");
        d.addCallMethod("fims/validation/lists/list/field", "setValue", 0);
        d.addCallMethod("fims/validation/lists/list/field/definition", "setDefinition", 0);

        // Create column objects
        d.addObjectCreate("fims/validation/worksheet/column", ColumnTrash.class);
        d.addSetProperties("fims/validation/worksheet/column");
        d.addSetNext("fims/validation/worksheet/column", "addColumn");

        try {
            d.parse(configFile);
            // add default rules here
            for (Worksheet ws: worksheets) {
                // run the "validDataTypeFormat" Rule for every worksheet
                Rule validDataTypeFormatRule = new Rule(mapping);
                validDataTypeFormatRule.setLevel("error");
                validDataTypeFormatRule.setType("validDataTypeFormat");
                ws.addRule(validDataTypeFormatRule);
            }
        } catch (IOException e) {
            throw new FimsRuntimeException(500, e);
        } catch (SAXException e) {
            throw new FimsRuntimeException(500, e);
        }
    }
}