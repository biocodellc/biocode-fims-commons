package biocode.fims.digester;

import biocode.fims.fimsExceptions.FimsRuntimeException;
import biocode.fims.validation.messages.RowMessage;
import biocode.fims.settings.FimsPrinter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import org.apache.commons.digester3.Digester;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;


/**
 * digester.Validation class holds all worksheets that are part of this validator
 */
public class Validation {
    // Loop all the worksheets associated with the validation element
    private final LinkedList<Worksheet> worksheets = new LinkedList<Worksheet>();
    // Loop all the lists associated with the validation element
    private final LinkedList<List> lists = new LinkedList<List>();
    // A SQL Lite connection is mainted by the validation class so we can run through the various rules
    @JsonIgnore
    private java.sql.Connection connection = null;
    @JsonIgnore
    private static Logger logger = LoggerFactory.getLogger(Validation.class);
    // File reference for a sqlite Database
    @JsonIgnore
    private File sqliteFile;

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
     *
     * @param column
     * @param sheetname
     * @return
     */
    public List findListForColumn(String column, String sheetname) {
        Worksheet sheet = null;
        // Get a list of rules for the first digester.Worksheet instance
        for (Worksheet s : worksheets) {
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
     *
     * @return k, v map of sheetName: RowMessage list
     */
    @JsonIgnore
    public HashMap<String, LinkedList<RowMessage>> getMessages() {
        HashMap<String, LinkedList<RowMessage>> messages = new HashMap<>();
        for (Iterator<Worksheet> w = worksheets.iterator(); w.hasNext(); ) {
            Worksheet worksheet = w.next();
            messages.put(worksheet.getSheetname(), worksheet.getMessages());
        }
        return messages;
    }

    public boolean hasErrors() {
        for (Worksheet w : worksheets) {
            if (!w.errorFree()) {
                return true;
            }
        }

        return false;
    }

    public boolean hasWarnings() {
        for (Worksheet w : worksheets) {
            if (!w.warningFree()) {
                return true;
            }
        }

        return false;
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
    @Deprecated
    public synchronized void addValidationRules(File configFile, Mapping mapping) {
        Digester d = new Digester();
        d.push(this);

        // Create worksheet objects
        d.addObjectCreate("fims/validation/worksheet", Worksheet.class);
        d.addSetProperties("fims/validation/worksheet");
        d.addSetNext("fims/validation/worksheet", "addWorksheet");

        // Create rule objects
        d.addObjectCreate("fims/validation/worksheet/rule", Rule.class);
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
            addDefaultRulesToWorksheets(mapping);
        } catch (IOException | SAXException e) {
            throw new FimsRuntimeException(500, e);
        }
    }

    private void addDefaultRulesToWorksheets(Mapping mapping) {
        for (Worksheet ws : worksheets) {

            // every entity uniqueKey should have the validForURI Rule run
            for (Rule rule : createValidForURIRules(mapping, ws.getSheetname())) {
                ws.addRule(rule);
            }

            // run the "validDataTypeFormat" Rule for every worksheet
            ws.addRule(createValidDataTypeFormatRule(mapping));
        }
    }

    private java.util.List<Rule> createValidForURIRules(Mapping mapping, String sheetname) {
        java.util.List<Rule> rules = new ArrayList<>();

        for (Entity entity : mapping.getEntities()) {
            if (addValidForUriRule(sheetname, entity)) {
                rules.add(createValidForURIRule(mapping, entity.getUniqueKey()));
            }
        }

        return rules;
    }

    private boolean addValidForUriRule(String sheetname, Entity entity) {
        return entity.hasWorksheet() && !entity.isValueObject() && entity.getWorksheet().equals(sheetname);
    }

    private Rule createValidDataTypeFormatRule(Mapping mapping) {
        Rule validDataTypeFormatRule = new Rule(mapping);
        validDataTypeFormatRule.setLevel("error");
        validDataTypeFormatRule.setType("validDataTypeFormat");
        return validDataTypeFormatRule;
    }

    private Rule createValidForURIRule(Mapping mapping, String column) {
        Rule validForURIRule = new Rule(mapping);
        validForURIRule.setColumn(mapping.getRootEntity().getUniqueKey());
        validForURIRule.setLevel("error");
        validForURIRule.setColumn(column);
        validForURIRule.setType("validForURI");
        return validForURIRule;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Validation)) return false;

        Validation that = (Validation) o;

        if (getWorksheets() != null ? !getWorksheets().equals(that.getWorksheets()) : that.getWorksheets() != null)
            return false;
        return getLists() != null ? getLists().equals(that.getLists()) : that.getLists() == null;
    }

    @Override
    public int hashCode() {
        int result = getWorksheets() != null ? getWorksheets().hashCode() : 0;
        result = 31 * result + (getLists() != null ? getLists().hashCode() : 0);
        return result;
    }
}