package biocode.fims.digester;

/**
 * Hold contents of a Field that is part of a List
 */
public class Field {
    private String uri;
    private String value;
    private String defined_by;


    public String getUri() {
        return uri;
    }

    public void setUri(String uri) {
        this.uri = uri;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public String getDefined_by() {
        return defined_by;
    }

    public void setDefined_by(String defined_by) {
        this.defined_by = defined_by;
    }
}


