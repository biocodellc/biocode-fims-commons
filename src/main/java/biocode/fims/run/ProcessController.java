package biocode.fims.run;


/**
 * Tracks status of data validation.  Helpful especially in a stateless environment.
 * This class is meant to be read/written as an attribute for an HTTPSession when
 * working in a Servlet environment.
 */
public class ProcessController {
    private DatasetProcessor datasetProcessor;
    private String expeditionCode;
    private int projectId;
    private int userId;
    private StringBuilder statusSB = new StringBuilder();
    private Boolean publicStatus = false;   // default to false

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

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public String getExpeditionCode() {
        return expeditionCode;
    }

    public int getProjectId() {
        return projectId;
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

    public void setDatasetProcessor(DatasetProcessor datasetProcessor) {
        this.datasetProcessor = datasetProcessor;
    }

    public DatasetProcessor getDatasetProcessor() {
        return datasetProcessor;
    }
}
