package biocode.fims.run;

import biocode.fims.digester.Validation;
import biocode.fims.digester.Mapping;
import biocode.fims.entities.Project;
import biocode.fims.entities.User;
import biocode.fims.fasta.FastaManager;
import biocode.fims.renderers.RowMessage;
import biocode.fims.utils.Html2Text;
import biocode.fims.utils.StringGenerator;
import ch.lambdaj.group.Group;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.*;

import static ch.lambdaj.Lambda.*;

/**
 * Tracks status of data validation.  Helpful especially in a stateless environment.
 * This class is meant to be read/written as an attribute for an HTTPSession when
 * working in a Servlet environment.
 */
public class ProcessController {
    private Boolean hasErrors = false;
    private Boolean hasWarnings = false;
    private StringBuilder commandLineWarningsSB;
    private Boolean clearedOfWarnings = false;
    private Boolean expeditionAssignedToUserAndExists = false;   // checks that the userId is authenticated against the supplied expedition
    private Boolean expeditionCreateRequired = false;
    private Boolean validated = false;
    private String inputFilename;
    private String expeditionCode;
    private String expeditionTitle;
    private int projectId;
    private int userId;
    private Validation validation;
    private Mapping mapping;
    private FastaManager fastaManager;
    private String worksheetName;
    // Store all messages as a k,v pair of sheetName: messages
    private HashMap<String, List<RowMessage>> messages = new HashMap<>();
    private StringBuilder statusSB = new StringBuilder();
    private String accessionNumber;
    private String defaultSheetUniqueKey;
    private Boolean publicStatus = false;   // default to false
    private Boolean finalCopy = false;
    private static Logger logger = LoggerFactory.getLogger(ProcessController.class);

    public String getWorksheetName() {
        return worksheetName;
    }

    public void setWorksheetName(String worksheetName) {
        this.worksheetName = worksheetName;
    }

    public StringBuilder getStatusSB() {
        return statusSB;
    }

    public void appendStatus(String s) {
        statusSB.append(stringToHTMLJSON(s));
    }

    public ProcessController(int projectId, String expeditionCode) {
        this.expeditionCode = expeditionCode;
        this.projectId = projectId;
    }

    public ProcessController() {

    }
    public int getUserId() { return userId; }

    public void setUserId(int userId) { this.userId = userId; }
    public Boolean isExpeditionCreateRequired() {
        return expeditionCreateRequired;
    }

    public void setExpeditionCreateRequired(Boolean expeditionCreateRequired) {
        this.expeditionCreateRequired = expeditionCreateRequired;
    }

    public Boolean getHasWarnings() {
        return hasWarnings;
    }

    public void setHasWarnings(Boolean hasWarnings) {
        this.hasWarnings = hasWarnings;
    }

    public Boolean getHasErrors() {
        return hasErrors;
    }

    public void setHasErrors(Boolean hasErrors) {
        this.hasErrors = hasErrors;
    }

    public Boolean isClearedOfWarnings() {
        return clearedOfWarnings;
    }

    public void setClearedOfWarnings(Boolean clearedOfWarnings) {
        this.clearedOfWarnings = clearedOfWarnings;
    }

    public Boolean isValidated() {
        return validated;
    }

    public void setValidated(Boolean validated) {
        this.validated = validated;
    }

    public String getInputFilename() {
        return inputFilename;
    }

    public void setInputFilename(String inputFilename) {
        this.inputFilename = inputFilename;
    }

    public String getExpeditionCode() {
        return expeditionCode;
    }

    public String getExpeditionTitle() {
        return expeditionTitle;
    }

    public void setExpeditionTitle(String expeditionTitle) {
        this.expeditionTitle = expeditionTitle;
    }

    public int getProjectId() {
        return projectId;
    }

    public Boolean isExpeditionAssignedToUserAndExists() {
        return expeditionAssignedToUserAndExists;
    }

    public void setExpeditionAssignedToUserAndExists(Boolean expeditionAssignedToUserAndExists) {
        this.expeditionAssignedToUserAndExists = expeditionAssignedToUserAndExists;
    }

    public Validation getValidation() {
        return validation;
    }

    public void setValidation(Validation validation) {
        this.validation = validation;
    }


    public Boolean getPublicStatus() {
        return publicStatus;
    }

    public void setPublicStatus(Boolean publicStatus) {
        this.publicStatus = publicStatus;
    }

    /**
     * Tells whether the given filename is ready to upload
     *
     * @return
     */
    public Boolean isReadyToUpload() {
        if (expeditionAssignedToUserAndExists &&
                validated &&
                inputFilename != null &&
                expeditionCode != null &&
                projectId > 0)
            return true;
        else
            return false;
    }

    /**
     * return a string that is to be used in html and is json safe
     *
     * @param s
     *
     * @return
     */
    public String stringToHTMLJSON(String s) {
        s = s.replaceAll("\n", "<br>").replaceAll("\t", "");
        return JSONObject.escape(s);
    }


    public String printStatus() {
        String retVal = "";
        retVal += "\tprojectId = " + projectId + "\n";
        retVal += "\texpeditionCode = " + expeditionCode + "\n";
        retVal += "\tinputFilename = " + inputFilename + "\n";

        if (clearedOfWarnings)
            retVal += "\tclearedOfWarnings=true\n";
        else
            retVal += "\tclearedOfWarnings=true\n";
        if (hasWarnings)
            retVal += "\thasWarnings=true\n";
        else
            retVal += "\thasWarnings=true\n";
        if (expeditionAssignedToUserAndExists)
            retVal += "\texpeditionAssignedToUser=true\n";
        else
            retVal += "\texpeditionAssignedToUser=false\n";
        if (validated)
            retVal += "\tvalidated=true\n";
        else
            retVal += "\tvalidated=false\n";

        return retVal;
    }

    /**
     * take an InputStream and extension and write it to a file in the operating systems temp dir.
     *
     * @param is
     * @param ext
     *
     * @return
     */
    public String saveTempFile(InputStream is, String ext) {
        String tempDir = System.getProperty("java.io.tmpdir");
        File f = new File(tempDir, new StringGenerator().generateString(20) + '.' + ext);

        try {
            OutputStream os = new FileOutputStream(f);
            try {
                byte[] buffer = new byte[4096];
                for (int n; (n = is.read(buffer)) != -1; )
                    os.write(buffer, 0, n);
            } finally {
                os.close();
            }
        } catch (IOException e) {
            logger.warn("IOException", e);
            return null;
        }
        return f.getAbsolutePath();
    }

    public void setAccessionNumber(String accessionNumber) {
        this.accessionNumber = accessionNumber;
    }

    public String getAccessionNumber() {
        return accessionNumber;
    }

    public void setDefaultSheetUniqueKey(String defaultSheetUniqueKey) {
        this.defaultSheetUniqueKey = defaultSheetUniqueKey;
    }

    public String getDefaultSheetUniqueKey() {
        return defaultSheetUniqueKey;
    }

    public void setCommandLineSB(StringBuilder commandLineWarningsSB) {
        this.commandLineWarningsSB = commandLineWarningsSB;
    }

    public StringBuilder getCommandLineSB() {
        return commandLineWarningsSB;
    }
    public void setFinalCopy(Boolean finalCopy) {
        this.finalCopy = finalCopy;
    }

    public boolean getFinalCopy() {
        return finalCopy;
    }

    public FastaManager getFastaManager() {
        return fastaManager;
    }

    public void setFastaManager(FastaManager fastaManager) {
        this.fastaManager = fastaManager;
    }

    public Mapping getMapping() {
        return mapping;
    }

    public void setMapping(Mapping mapping) {
        this.mapping = mapping;
    }

    public void addMessages(HashMap<String, LinkedList<RowMessage>> map) {
        messages.putAll(map);
    }

    /**
     * print messages for the command line
     * @return
     */
    public String printMessages() {
        // Create a simplified output stream just for commandline printing.
        StringBuilder commandLineWarningSB = new StringBuilder();
        Html2Text htmlParser = new Html2Text();

        // iterate through each sheet in messages and append associated messages
        Iterator it = messages.keySet().iterator();
        while (it.hasNext()) {
            String sheetName = (String) it.next();

            // Group all Messages using lambdaj jar library
            Group<RowMessage> rowGroup = group(messages.get(sheetName), by(on(RowMessage.class).getGroupMessageAsString()));
            for (String key : rowGroup.keySet()) {
                java.util.List<RowMessage> rowMessageList = rowGroup.find(key);

                // Parse the Row Messages that are meant for HTML display
                commandLineWarningSB.append(htmlParser.convert(key)+"\n");

                for (RowMessage m : rowMessageList) {
                    commandLineWarningSB.append("\t" + m.print() + "\n");
                }
            }

        }
        return commandLineWarningSB.toString();
    }

    /**
     * get messages as JSON
     * @return
     */
    public JSONArray getMessages() {
        JSONArray messageArray = new JSONArray();

        // iterate through each sheet in messages and append associated messages
        Iterator it = messages.keySet().iterator();
        while (it.hasNext()) {
            String sheetName = (String) it.next();
            JSONObject sheetMessages = new JSONObject();
            JSONArray warningMessages = new JSONArray();
            JSONArray errorMessages = new JSONArray();

            // Group all Messages using lambdaj jar library
            Group<RowMessage> rowGroup = group(messages.get(sheetName), by(on(RowMessage.class).getGroupMessage()));
            for (String key : rowGroup.keySet()) {
                JSONArray warningGroupArray = new JSONArray();
                JSONArray errorGroupArray = new JSONArray();
                java.util.List<RowMessage> rowMessageList = rowGroup.find(key);

                for (RowMessage m : rowMessageList) {
                    if (m.getLevel() == RowMessage.ERROR) {
                        errorGroupArray.add(m.print());
                    } else {
                        warningGroupArray.add(m.print());
                    }
                }
                if (errorGroupArray.size() > 0) {
                    JSONObject group = new JSONObject();
                    group.put(key, errorGroupArray);
                    errorMessages.add(group);
                }
                if (warningGroupArray.size() > 0) {
                    JSONObject group = new JSONObject();
                    group.put(key, warningGroupArray);
                    warningMessages.add(group);
                }
            }

            sheetMessages.put("warnings", warningMessages);
            sheetMessages.put("errors", errorMessages);
            JSONObject sheet = new JSONObject();
            sheet.put(sheetName, sheetMessages);
            messageArray.add(sheet);
        }

        return messageArray;
    }

    public void addMessage(String sheetName, RowMessage rowMessage) {
        messages.get(sheetName).add(rowMessage);
    }

}
