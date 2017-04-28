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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Metadata)) return false;

        Metadata metadata = (Metadata) o;

        if (getDoi() != null ? !getDoi().equals(metadata.getDoi()) : metadata.getDoi() != null) return false;
        if (getShortname() != null ? !getShortname().equals(metadata.getShortname()) : metadata.getShortname() != null)
            return false;
        if (getEml_location() != null ? !getEml_location().equals(metadata.getEml_location()) : metadata.getEml_location() != null)
            return false;
        if (getTarget() != null ? !getTarget().equals(metadata.getTarget()) : metadata.getTarget() != null)
            return false;
        if (getQueryTarget() != null ? !getQueryTarget().equals(metadata.getQueryTarget()) : metadata.getQueryTarget() != null)
            return false;
        if (getExpeditionForwardingAddress() != null ? !getExpeditionForwardingAddress().equals(metadata.getExpeditionForwardingAddress()) : metadata.getExpeditionForwardingAddress() != null)
            return false;
        if (getDatasetForwardingAddress() != null ? !getDatasetForwardingAddress().equals(metadata.getDatasetForwardingAddress()) : metadata.getDatasetForwardingAddress() != null)
            return false;
        if (getNmnh() != null ? !getNmnh().equals(metadata.getNmnh()) : metadata.getNmnh() != null) return false;
        if (getOwlRestrictionFile() != null ? !getOwlRestrictionFile().equals(metadata.getOwlRestrictionFile()) : metadata.getOwlRestrictionFile() != null)
            return false;
        return getTextAbstract() != null ? getTextAbstract().equals(metadata.getTextAbstract()) : metadata.getTextAbstract() == null;
    }

    @Override
    public int hashCode() {
        int result = getDoi() != null ? getDoi().hashCode() : 0;
        result = 31 * result + (getShortname() != null ? getShortname().hashCode() : 0);
        result = 31 * result + (getEml_location() != null ? getEml_location().hashCode() : 0);
        result = 31 * result + (getTarget() != null ? getTarget().hashCode() : 0);
        result = 31 * result + (getQueryTarget() != null ? getQueryTarget().hashCode() : 0);
        result = 31 * result + (getExpeditionForwardingAddress() != null ? getExpeditionForwardingAddress().hashCode() : 0);
        result = 31 * result + (getDatasetForwardingAddress() != null ? getDatasetForwardingAddress().hashCode() : 0);
        result = 31 * result + (getNmnh() != null ? getNmnh().hashCode() : 0);
        result = 31 * result + (getOwlRestrictionFile() != null ? getOwlRestrictionFile().hashCode() : 0);
        result = 31 * result + (getTextAbstract() != null ? getTextAbstract().hashCode() : 0);
        return result;
    }
}
