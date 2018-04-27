package biocode.fims.digester;

import biocode.fims.validation.messages.RowMessage;
import biocode.fims.settings.FimsPrinter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.sql.SQLException;
import java.util.*;
import java.util.List;

/**
 * digester.Worksheet class holds all elements pertaining to worksheets including most importantly
 * rules and columns.  Rules define all the validation rules associated with this worksheet and
 * columns define all of the column names to be found in this worksheet.  The Worksheet class also
 * defines a LinkedList of messages which store all processing messages (errors or warnings) when
 * running through this worksheet's rules.
 */
public class Worksheet {

    // The name of this worksheet (as defined by the spreadsheet)
    private String sheetname;
    // Store the rules associated with this worksheet
    private final List<Rule> rules = new ArrayList<Rule>();
    // Store the validation object, passed into the run method
    @JsonIgnore
    private Validation validation = null;
    // Store all messages related to this Worksheet
    @JsonIgnore
    private LinkedList<RowMessage> messages = new LinkedList<RowMessage>();
    // Store the reference for the columns associated with this worksheet
    @JsonIgnore
    private final List<ColumnTrash> columns = new ArrayList<ColumnTrash>();

    @JsonIgnore
    private static Logger logger = LoggerFactory.getLogger(Worksheet.class);
    /**
     * Add columns element to the worksheet element
     *
     * @param column
     */
    public void addColumn(ColumnTrash column) {
        columns.add(column);
    }

    /**
     * Get all the processing/validation messages associated with this worksheet
     *
     * @return
     */
    public LinkedList<RowMessage> getMessages() {
        return messages;
    }

    /**
     * Get a reference to the validation object.  This is useful when working with
     * worksheets and rules to reference objects belonging to the validation object,
     * in particular lists.
     *
     * @return
     */
    public Validation getValidation() {
        return validation;
    }

    /**
     * Set the name of this worksheet
     *
     * @param sheetname
     */
    public void setSheetname(String sheetname) {
        this.sheetname = sheetname;
    }

    /**
     * Get the name of this worksheet
     *
     * @return
     */
    public String getSheetname() {
        return sheetname;
    }

    /**
     * Add a rule for this worksheet
     *
     * @param rule
     */
    public void addRule(Rule rule) {
        rules.add(rule);
    }

    public List<Rule> getRules() {
        return rules;
    }

    public void print() {
        FimsPrinter.out.println("  sheetname=" + sheetname);

        FimsPrinter.out.println("  rules ... ");
        for (Iterator<Rule> i = rules.iterator(); i.hasNext(); ) {
            Rule r = i.next();
            r.print();
        }

        FimsPrinter.out.println("  columns ... ");
        for (Iterator<ColumnTrash> i = columns.iterator(); i.hasNext(); ) {
            ColumnTrash c = i.next();
            c.print();
        }
    }

    /**
     * Print metadata on all the rules contained in this worksheet
     * @return
     */
    /*
    public String printRuleMetadata() {
        StringBuilder output = new StringBuilder();

        Iterator rulesIt = rules.iterator();
        while (rulesIt.hasNext()) {
            Rule r = (Rule) rulesIt.next();
            output.append("<li>\n");
            //
            String prettyTypeName = r.getType();
            if (r.getType().equals("checkInXMLFields")) {
                prettyTypeName = "Lookup Value From List";
            }
            // Display the Rule type
            output.append("\t<li>type: " + r.getType() + "</li>\n");
            // Display warning levels
            output.append("\t<li>level: " + r.getLevel() + "</li>\n");
            // Display values
            if (r.getValue() != null) {
                output.append("\t<li>value: " + r.getValue() + "</li>\n");
            }
            // Display fields
            Iterator it = null, listIt = null;
            try {
                it = r.getFields().iterator();
            } catch (Exception e) {
                // Null List
            }
            try {
                listIt = r.getListElements().iterator();
            } catch (Exception e) {
                // Null List
            }
            boolean someValuesInList = false;
            // One or the other types of list need data
            if (it != null || listIt != null &&
                    (it.hasNext() || listIt.hasNext())) {
                someValuesInList = true;

            }
            if (someValuesInList)
                output.append("\t<li>list: \n");

            // Look at the Fields
            if (it != null) {
                while (it.hasNext()) {
                    String field = (String) it.next();
                    output.append("\t\t<li>" + field + "</li>\n");
                }
            }
            // Now look at lists
            if (listIt != null) {
                while (listIt.hasNext()) {
                    String field = (String) listIt.next();
                    output.append("\t\t<li>" + field + "</li>\n");
                }
            }

            if (someValuesInList)
                output.append("\t</li>\n");

            output.append("</li>\n");
        }
        return output.toString();
    }
       */
    /**
     * Return a list of columns given a list alias.  This shows us ALL of the columns that may reference
     * a particular list
     *
     * @param list
     *
     * @return
     */
    public ArrayList<String> getColumnsForList(String list) {
        ArrayList columns = new ArrayList();
        for (Iterator<Rule> i = rules.iterator(); i.hasNext(); ) {
            Rule r = i.next();
            if (list.equals(r.getList())) {
                columns.add(r.getColumn());
            }
        }
        return columns;
    }

    /**
     * Indicate whether this worksheet is error free or not
     *
     * @return true if this worksheet is clean
     */
    public boolean errorFree() {
        // Check all messages to see if any type of error has been found
        for (Iterator<RowMessage> m = messages.iterator(); m.hasNext(); ) {
            if (m.next().getLevel() == RowMessage.ERROR)
                return false;
        }
        return true;
    }

    /**
     * Indicate whether this worksheet is warning free or not
     *
     * @return true if this worksheet is clean
     */
    public boolean warningFree() {
        // Check all messages to see if any type of error has been found
        for (Iterator<RowMessage> m = messages.iterator(); m.hasNext(); ) {
            if (m.next().getLevel() == RowMessage.WARNING)
                return false;
        }
        return true;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Worksheet)) return false;

        Worksheet worksheet = (Worksheet) o;

        if (getSheetname() != null ? !getSheetname().equals(worksheet.getSheetname()) : worksheet.getSheetname() != null)
            return false;
        return getRules() != null ? getRules().equals(worksheet.getRules()) : worksheet.getRules() == null;
    }

    @Override
    public int hashCode() {
        int result = getSheetname() != null ? getSheetname().hashCode() : 0;
        result = 31 * result + (getRules() != null ? getRules().hashCode() : 0);
        return result;
    }
}
