package biocode.fims.config;

import biocode.fims.digester.DataType;
import biocode.fims.utils.EnumUtils;
import org.apache.commons.lang.StringUtils;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.*;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Array;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The publicly accessible tests return or true or false, with true indicating success and false indicating that
 * the encodeURIcomponent was failed.  All messages are managed by the ConfigurationFileErrorMessager class and can be
 * retrieved at any point to display any explanatory information regarding why a particular encodeURIcomponent failed.
 * If all
 * tests pass then no messages are written to the ConfigurationFileErrorMessager
 */
public class ConfigurationFileTester {
    private final static Logger logger = LoggerFactory.getLogger(ConfigurationFileTester.class);
    DocumentBuilder builder = null;
    Document document = null;
    public File fileToTest = null;
    private ConfigurationFileErrorMessager messages = new ConfigurationFileErrorMessager();

    /**
     * Return all the messages from this Configuration File Test
     */
    public String getMessages() {
        return messages.printMessages();
    }

    /**
     * Test that we can initialize the document
     *
     * @param fileToTest
     *
     * @return
     */
    @Test
    public boolean init(File fileToTest) {
        this.fileToTest = fileToTest;
        Document document;
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setValidating(false);
            factory.setNamespaceAware(true);

            builder = factory.newDocumentBuilder();

            builder.setErrorHandler(new ConfigurationFileErrorHandler());

        } catch (ParserConfigurationException e) {
            logger.warn("", e);
            return false;
        }
        return true;
    }

    /**
     * Test parsing the file
     *
     *
     * @return
     */
    @Test
    public boolean parse() {
        // A simple first check that the document is valid
        try {
            document = builder.parse(new InputSource(fileToTest.getAbsoluteFile().toString()));
        } catch (IOException|SAXException e) {
            logger.warn("", e);
            return false;
        }
        return true;
    }

    /**
     * Check the structure of our lists
     *
     * @return StringBuilder
     *
     * @throws ConfigurationFileError
     */
    public boolean checkLists() {
        if (!parse()) {
            return false;
        }

        boolean passedTest = true;
        ArrayList listAliases = new ArrayList();
        // Loop Rules
        NodeList lists = document.getElementsByTagName("list");
        for (int i = 0; i < lists.getLength(); i++) {
            NamedNodeMap listAttributes = lists.item(i).getAttributes();
            listAliases.add(listAttributes.getNamedItem("alias").getNodeValue());
        }

        // Build an array of CheckInXMLFields Rules
        ArrayList rulesCheckInXMLFields = new ArrayList();
        NodeList rules = document.getElementsByTagName("rule");
        for (int i = 0; i < rules.getLength(); i++) {
            if (rules.item(i) != null) {
                NamedNodeMap ruleAttributes = rules.item(i).getAttributes();
                if (ruleAttributes != null &&
                        ruleAttributes.getNamedItem("type") != null &&
                        ((ruleAttributes.getNamedItem("type").getNodeValue().equalsIgnoreCase("CheckInXMLFields")
                                || ruleAttributes.getNamedItem("type").getNodeValue().equalsIgnoreCase("controlledVocabulary")))) {
                    String list = ruleAttributes.getNamedItem("list").getNodeValue() + "|" + ruleAttributes.getNamedItem("column").getNodeValue();
                    rulesCheckInXMLFields.add(list);
                }
            }
        }

        Iterator it = rulesCheckInXMLFields.iterator();
        while (it.hasNext()) {
            String ruleListName = (String) it.next();
            String[] parsed = ruleListName.split("\\|");
            String listname = parsed[0];
            String columnname = "";

            if (parsed.length > 1)
                columnname = " for column " + parsed[1];

            if (!listAliases.contains(listname)) {
                messages.add(this, listname + " is specified by a rule as a list" + columnname + ", but was not named as a list", "checkList");
                passedTest = false;
            }
        }

        return passedTest;
    }

    /**
     * Run all configuration checks
     * @return
     */
    public boolean isValidConfig() {
        return (parse() && checkUniqueKeys() && checkDatatypes());
    }

    /**
     * verify that attribute datatype is a valid value
     * @return
     */
    private boolean checkDatatypes() {
        boolean validDataTypes = true;
        NodeList attributes = document.getElementsByTagName("attribute");

        ArrayList<String> invalidDataTypes = new ArrayList<>();
        ArrayList<String> invalidDataFormat = new ArrayList<>();

        for (int i=0; i < attributes.getLength(); i++) {
            Element attribute = (Element) attributes.item(i);
            String dataTypeString = attribute.getAttribute("datatype");

            if (dataTypeString != null) {
                DataType dataType = EnumUtils.lookup(DataType.class, dataTypeString);
                if (dataType == null) {
                    invalidDataTypes.add(attribute.getAttribute("column"));
                } else {
                    // if DATETIME DataType, then we need a dataformat as well
                    if (dataType == DataType.DATETIME) {
                        if (attribute.getAttribute("dataformat").trim().isEmpty()) {
                            invalidDataFormat.add(attribute.getAttribute("column"));
                        }
                    }
                }
            }

        }

        if (invalidDataTypes.size() > 0) {
            messages.add(this, "The attributes with the columns [" + StringUtils.join(invalidDataTypes, ",") + "] have invalid datatypes", "checkDatatypes");
            validDataTypes = false;

        }
        if (invalidDataFormat.size() > 0) {
            messages.add(this, "The attributes with the columns [" + StringUtils.join(invalidDataTypes, ",") + "] have invalid dataformats", "checkDatatypes");
            validDataTypes = false;
        }

        return validDataTypes;
    }

    /**
     * Test that the configuration file is OK!
     *
     * @return
     */
    public boolean checkUniqueKeys() {
        String worksheetUniqueKey = "";
        boolean passedTest = true;

        // Loop Rules
        NodeList rules = document.getElementsByTagName("rule");
        ArrayList<String> uniqueKeys = getUniqueValueRules(rules);

        ArrayList<String> requiredColumns = getRequiredValueThrowsErrorRules(rules);
        //System.out.println("REQUIRED COLUMNS = " + requiredColumns);

        // Loop Entities
        NodeList entities = document.getElementsByTagName("entity");
        // atLeastOneUniqueKey
        boolean atLeastOneUniqueKeyFound = false;
        boolean atLeastOneRequiredColumnFound = false;
        // Loop Entities
        for (int i = 0; i < entities.getLength(); i++) {
            NamedNodeMap entityAttributes = entities.item(i).getAttributes();

            // Check worksheetUniqueKeys
            worksheetUniqueKey = entityAttributes.getNamedItem("worksheetUniqueKey").getNodeValue();

            if (uniqueKeys.contains(worksheetUniqueKey)) {
                atLeastOneUniqueKeyFound = true;
            }

            if (requiredColumns.contains(worksheetUniqueKey)) {
                atLeastOneRequiredColumnFound = true;
            }

            // construct a List to hold URI values to check they are unique within the node
            List<String> uriList = new ArrayList<String>();

            // Check the attributes for this entity
            List entityChildren = elements(entities.item(i));
            for (int j = 0; j < entityChildren.size(); j++) {
                Element attributeElement = (Element) entityChildren.get(j);

                if (attributeElement != null) { //&& node.getNodeName().equals("attribute")) {

                    NamedNodeMap attributes = attributeElement.getAttributes();//.getNamedItem("uri");

                    // Check the URI field by populating the uriList with values
                    String uri = null;
                    try {
                        uri = attributes.getNamedItem("uri").getNodeValue().toString();
                    } catch (NullPointerException e) {
                    }
                    if (uri != null) {
                        uriList.add(uri);
                    }

                    String column = null;
                    try {
                        column = attributes.getNamedItem("column").getNodeValue();
                    } catch (NullPointerException e) {
                    }
                    if (column != null) {
                        if (!checkSpecialCharacters(column)) {
                            passedTest = false;
                        }
                    }


                }

            }
            // Run the URI list unique Value check for each entity
            if (!checkUniqueValuesInList("URI attribute value", uriList)) {
                passedTest = false;
            }
        }

        // Tell is if atLeastOneUniqueKey is not found.
        if (!atLeastOneUniqueKeyFound) {
            messages.add(this, "Worksheet unique key = '" + worksheetUniqueKey + "' does not have uniqueValue rule", "atLeastOneUniqueKeyFound");
            passedTest = false;
        }

        // Tell is if atLeastOneUniqueKey is not found.
        if (!atLeastOneRequiredColumnFound) {
            messages.add(this, "Worksheet unique key = '" + worksheetUniqueKey + "' is not defined as a Required Column with level = 'error'", "atLeastOneRequiredColumnFound");
            passedTest = false;
        }

        return passedTest;
    }

    /**
     * Construct a list of elements given a parent node, ensuring that only element children are returned.
     *
     * @param parent
     *
     * @return
     */
    private List<Element> elements(Node parent) {
        List<Element> result = new LinkedList<Element>();
        NodeList nl = parent.getChildNodes();
        for (int i = 0; i < nl.getLength(); ++i) {
            if (nl.item(i).getNodeType() == Node.ELEMENT_NODE)
                result.add((Element) nl.item(i));
        }
        return result;
    }

    /**
     * Check that a particular list has unique values
     *
     * @param message
     * @param list
     */
    private boolean checkUniqueValuesInList(String message, List<String> list) {
        boolean passedTest = true;

        Set<String> uniqueSet = new HashSet<String>(list);
        for (String temp : uniqueSet) {
            Integer count = Collections.frequency(list, temp);
            if (count > 1) {
                messages.add(this, message + " " + temp + " used more than once", "checkUniqueValuesInList");
                passedTest = false;
            }
        }
        return passedTest;
    }

    /**
     * Check for special characters in a string
     *
     * @param stringToCheck
     */
    private boolean checkSpecialCharacters(String stringToCheck) {
        boolean passedTest = true;
        // Check worksheetUniqueKeys
        //String column = attributeAttributes.getNamedItem("column").getNodeValue();
        Pattern p = Pattern.compile("[^a-z0-9 \\(\\)\\/\\,\\.\\_-]", Pattern.CASE_INSENSITIVE);
        Matcher m = p.matcher(stringToCheck);
        if (m.find()) {
            messages.add(this, "Column attribute value " + stringToCheck + " contains an invalid character", "checkSpecialCharacters");
            passedTest = false;
        }
        return passedTest;
    }


    /**
     * Check that Rules are well-formed
     *
     * @return
     */
    public boolean checkRuleFormation() {
        // Build a NodeList of Rules
        NodeList rules = document.getElementsByTagName("rule");

        Boolean passedTest = true;
        int length = rules.getLength();

        for (int n = 0; n < length; ++n) {
            NamedNodeMap attributes = rules.item(n).getAttributes();
            String type = attributes.getNamedItem("type").getNodeValue();
            String column = attributes.getNamedItem("column").getNodeValue();
            if (column == null || column.trim().equals("")) {
                if (type.equalsIgnoreCase("uniqueValue") ||
                        type.equalsIgnoreCase("requiredValueFromOtherColumn") ||
                        type.equalsIgnoreCase("validateNumeric") ||
                        type.equalsIgnoreCase("isNumeric") ||
                        type.equalsIgnoreCase("controlledVocabulary")) {
                    messages.add(this, "Missing column specification for a rule that requires it", "missingColumnValueForRule");

                    passedTest = false;
                }
            }
        }
        return passedTest;
    }

    /**
     * Return an ArrayList of rules that have unique values
     *
     * @param rules
     *
     * @return
     */
    private ArrayList<String> getUniqueValueRules(NodeList rules) {
        ArrayList<String> keys = new ArrayList<String>();
        int length = rules.getLength();
        Node[] copy = new Node[length];

        for (int n = 0; n < length; ++n) {
            NamedNodeMap attributes = rules.item(n).getAttributes();
            // Search all rules type=uniqueValue
            if (attributes.getNamedItem("type").getNodeValue().equals("uniqueValue")) {
                // Get the column name on this key
                String columnName = attributes.getNamedItem("column").getNodeValue();
                keys.add(columnName);
            }
        }

        return keys;
    }


    /**
     * Return an ArrayList of rules that have unique values
     *
     * @param rules
     *
     * @return
     */
    private ArrayList<String> getRequiredValueThrowsErrorRules(NodeList rules) {
        ArrayList<String> keys = new ArrayList<String>();
        int length = rules.getLength();
        Node[] copy = new Node[length];

        for (int n = 0; n < length; ++n) {
            Node node = rules.item(n);
            NamedNodeMap attributes = node.getAttributes();
            // Search all rules type=uniqueValue
            if (attributes.getNamedItem("type").getNodeValue().equals("RequiredColumns")) {
                // Get the column name on this key
                String level = attributes.getNamedItem("level").getNodeValue();
                // If this defines an error, then loop the entities

                if (level.equalsIgnoreCase("error")) {
                    NodeList nodeList = node.getChildNodes();
                    int lengthNodeList = nodeList.getLength();
                    for (int nl = 0; nl < lengthNodeList; nl++) {
                        Node nodeNodeList = nodeList.item(nl);
                        keys.add(nodeNodeList.getTextContent());
                    }
                }
            }
        }

        return keys;
    }

    /**
     * For testing purposes ONLY, call this script from unit_tests directory
     *
     * @param args
     *
     * @throws ConfigurationFileError
     */
    public static void main(String[] args) throws ConfigurationFileError {
        String output_directory = System.getProperty("user.dir") + File.separator + "sampledata" + File.separator;
        //File file = new File("/Users/jdeck/IdeaProjects/biocode-fims/web_nmnh/docs/SIBOT.xml");
        //File file = new File("/Users/jdeck/IdeaProjects/biocode-fims/sampledata/bwp_config_concat.xml");

        /*ConfigurationFileTester cFT = new ConfigurationFileTester();
        cFT.init(file);
        cFT.parse();
        cFT.checkLists();
        cFT.checkUniqueKeys();
        System.out.println(cFT.messages.printMessages());
        */

        // Check ACTIVE Project configuration files
        //Integer projects[] = {1, 3, 4, 5, 8, 9, 10, 11, 12, 22};
        String projects[] = {"BOT", "ENT", "INV", "MIN", "VZA", "VZB", "VZF", "VZM"};

        for (int i = 0; i < projects.length; i++) {
            String project = "SI" + projects[i];
            System.out.println("Configuration File Testing For Project = " + project);
            try {
                ConfigurationFileTester cFT = new ConfigurationFileTester();
                //File file = new ConfigurationFileFetcher(projectId, outputDirectory, true).getOutputFile();
                File file = new File("/Users/jdeck/IdeaProjects/biocode-fims/web_nmnh/docs/" + project + ".xml");
                System.out.println("Checking file " + file.toString());
                cFT.init(file);
                cFT.parse();
                cFT.checkLists();
                cFT.checkUniqueKeys();
                System.out.println(cFT.messages.printMessages());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }


        // Check for well-formedness -- this one passes
        /*
        ConfigurationFileTester cFT = new ConfigurationFileTester();

        try {
            cFT.testConfigFile(new File(outputDirectory + "testConfiguration1.xml"));
        } catch (ConfigurationFileError e) {
            System.out.println("Configuration File Construction Error Messages: \n" + e.getMessage());
        }

        // Check for unique keys
        try {
            cFT.testConfigFile(new File(outputDirectory + "testConfiguration2.xml"));
        } catch (ConfigurationFileError e) {
            System.out.println("Configuration File Construction Error Messages: \n" + e.getMessage());
        }
        */


    }
}
