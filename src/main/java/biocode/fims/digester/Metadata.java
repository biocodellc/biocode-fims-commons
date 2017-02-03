package biocode.fims.digester;

/**
 * Metadata defines metadata for this FIMS installation
 */
public class Metadata {
    private String doi;
    private String shortname;
    private String eml_location;
    private String target;
    private String queryTarget;
    private String expeditionForwardingAddress;
    private String datasetForwardingAddress;
    private String nmnh;
    private String owlRestrictionFile;

    private String textAbstract;

    public String getTarget() {
        return target;
    }

    public void setTarget(String target) {
        this.target = target;
    }

    public String getOwlRestrictionFile() { return owlRestrictionFile;}

    public void setOwlRestrictionFile(String owlRestrictionFile) {this.owlRestrictionFile = owlRestrictionFile;};

    public String getQueryTarget() {
        return queryTarget;
    }

    public void setQueryTarget(String queryTarget) {
        this.queryTarget = queryTarget;
    }

    public String getDoi() {
        return doi;
    }

    public void setDoi(String doi) {
        this.doi = doi;
    }

    public String getNmnh() {
        return nmnh;
    }

    public void setNmnh(String nmnh) {
        this.nmnh = nmnh;
    }

    public String getShortname() {
        return shortname;
    }

    public void setShortname(String shortname) {
        this.shortname = shortname;
    }

    public String getEml_location() {
        return eml_location;
    }

    public void setEml_location(String eml_location) {
        this.eml_location = eml_location;
    }


    public String getTextAbstract() {
        return textAbstract;
    }

    public void addTextAbstract(String textAbstract) {
        this.textAbstract = textAbstract;
    }

    public String getExpeditionForwardingAddress() {
        return expeditionForwardingAddress;
    }

    public void setExpeditionForwardingAddress(String expeditionForwardingAddress) {
        this.expeditionForwardingAddress = expeditionForwardingAddress;
    }

    public String getDatasetForwardingAddress() {
        return datasetForwardingAddress;
    }

    public void setDatasetForwardingAddress(String datasetForwardingAddress) {
        this.datasetForwardingAddress = datasetForwardingAddress;
    }

    @Override
    public String toString() {
        return "Metadata{" +
                "doi='" + doi + '\'' +
                ", shortname='" + shortname + '\'' +
                ", eml_location='" + eml_location + '\'' +
                ", target='" + target + '\'' +
                ", queryTarget='" + queryTarget + '\'' +
                ", expeditionForwardingAddress='" + expeditionForwardingAddress + '\'' +
                ", datasetForwardingAddress='" + datasetForwardingAddress + '\'' +
                ", nmnh='" + nmnh + '\'' +
                ", textAbstract='" + textAbstract + '\'' +
                '}';
    }
}
