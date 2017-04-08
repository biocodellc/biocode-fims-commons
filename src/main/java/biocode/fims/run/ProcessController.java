package biocode.fims.run;

import biocode.fims.digester.Validation;
import biocode.fims.digester.Mapping;
import biocode.fims.renderers.RowMessage;
import biocode.fims.utils.Html2Text;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.util.*;
import java.util.stream.Collectors;


/**
 * Tracks status of data validation.  Helpful especially in a stateless environment.
 * This class is meant to be read/written as an attribute for an HTTPSession when
 * working in a Servlet environment.
 */
public class ProcessController {
    private Boolean hasWarnings = false;
    private Process process;
    private String expeditionCode;
    private String expeditionTitle;
    private int projectId;
    private int userId;
    private Validation validation;
    private Mapping mapping;
    // Store all messages as a k,v pair of sheetName: messages
    private HashMap<String, List<RowMessage>> messages = new HashMap<>();
    private JSONObject configMessages;
    private StringBuilder statusSB = new StringBuilder();
    private StringBuilder successfullUploadSB = new StringBuilder();
    private String accessionNumber;
    private Boolean publicStatus = false;   // default to false
    private Boolean finalCopy = false;
    private String outputFolder;

    public ProcessController(int projectId, String expeditionCode) {
        this.expeditionCode = expeditionCode;
        this.projectId = projectId;
    }

    public StringBuilder getStatusSB() {
        return statusSB;
    }

    public void appendStatus(String s) {
        statusSB.append(stringToHTML(s));
    }

    public String getSuccessMessage() {
        return successfullUploadSB.toString();
    }

    public void appendSuccessMessage(String s) {
        successfullUploadSB.append(stringToHTML(s));
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public Boolean getHasWarnings() {
        return hasWarnings;
    }

    public void setHasWarnings(Boolean hasWarnings) {
        this.hasWarnings = hasWarnings;
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
     * return a string that is to be used in html
     *
     * @param s
     * @return
     */
    private String stringToHTML(String s) {
        s = s.replaceAll("\n", "<br>").replaceAll("\t", "");
        return s;
    }

    public void setAccessionNumber(String accessionNumber) {
        this.accessionNumber = accessionNumber;
    }

    public String getAccessionNumber() {
        return accessionNumber;
    }


    public void setFinalCopy(Boolean finalCopy) {
        this.finalCopy = finalCopy;
    }

    public boolean getFinalCopy() {
        return finalCopy;
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
     *
     * @return
     */
    public String printMessages() {
        // Create a simplified output stream just for commandline printing.
        StringBuilder commandLineWarningSB = new StringBuilder();
        Html2Text htmlParser = new Html2Text();

        if (configMessages != null) {
            commandLineWarningSB.append("Configuration File Errors: \n");

            JSONArray errorMessages = (JSONArray) ((JSONObject) configMessages.get("errors")).get("Configuration File");
            for (Object msg: errorMessages) {

                commandLineWarningSB.append("\t" + msg + "\n");

            }
        }

        // iterate through each sheet in messages and append associated messages
        Iterator it = messages.keySet().iterator();
        while (it.hasNext()) {
            String sheetName = (String) it.next();

            Map<String, List<RowMessage>> groupedRowMessages = messages.get(sheetName)
                    .stream()
                    .collect(
                            Collectors.groupingBy(RowMessage::getGroupMessage)
                    );

            for (String key : groupedRowMessages.keySet()) {
                java.util.List<RowMessage> rowMessageList = groupedRowMessages.get(key);

                // Parse the Row Messages that are meant for HTML display
                commandLineWarningSB.append(htmlParser.convert(key) + "\n");

                for (RowMessage m : rowMessageList) {
                    commandLineWarningSB.append("\t" + m.print() + "\n");
                }
            }

        }

        return commandLineWarningSB.toString();
    }

    /**
     * get messages as JSON
     *
     * @return
     */
    public JSONObject getMessages() {
        JSONObject worksheets = new JSONObject();

        // iterate through each sheet in messages and append associated messages
        Iterator it = messages.keySet().iterator();
        while (it.hasNext()) {
            String sheetName = (String) it.next();
            JSONObject sheetMessages = new JSONObject();
            JSONObject warningMessages = new JSONObject();
            JSONObject errorMessages = new JSONObject();

            Map<String, List<RowMessage>> groupedRowMessages = messages.get(sheetName)
                    .stream()
                    .collect(
                            Collectors.groupingBy(RowMessage::getGroupMessage)
                    );

            for (String key : groupedRowMessages.keySet()) {
                JSONArray warningGroupArray = new JSONArray();
                JSONArray errorGroupArray = new JSONArray();
                List<RowMessage> rowMessageList = groupedRowMessages.get(key);

                for (RowMessage m : rowMessageList) {
                    if (m.getLevel() == RowMessage.ERROR) {
                        errorGroupArray.add(m.print());
                    } else {
                        warningGroupArray.add(m.print());
                    }
                }
                if (errorGroupArray.size() > 0) {
                    errorMessages.put(key, errorGroupArray);
                }
                if (warningGroupArray.size() > 0) {
                    warningMessages.put(key, warningGroupArray);
                }
            }

            sheetMessages.put("warnings", warningMessages);
            sheetMessages.put("errors", errorMessages);
            worksheets.put(sheetName, sheetMessages);
        }

        JSONObject messages = new JSONObject();
        messages.put("worksheets", worksheets);
        messages.put("config", configMessages);
        return messages;
    }

    public void addMessage(String sheetName, RowMessage rowMessage) {
        messages.putIfAbsent(sheetName, new LinkedList<>());

        messages.get(sheetName).add(rowMessage);
    }

    public void setConfigMessages(JSONObject messages) {
        configMessages = messages;
    }

    public String getOutputFolder() {
        return outputFolder;
    }

    public void setOutputFolder(String outputFolder) {
        this.outputFolder = outputFolder;
    }

    public void setProcess(Process process) {
        this.process = process;
    }

    public Process getProcess() {
        return process;
    }

    public void setExpeditionCode(String expeditionCode) {
        this.expeditionCode = expeditionCode;
    }
}
