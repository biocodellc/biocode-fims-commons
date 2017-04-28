package biocode.fims.digester;

/**
 * Hold contents of a Field that is part of a List
 */
public class Field {
    private String uri;
    private String value;
    private String defined_by;
    private String definition;


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

    public String getDefinition() {
        return definition;
    }

    public void setDefinition(String definition) {
        this.definition = definition;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Field)) return false;

        Field field = (Field) o;

        if (getUri() != null ? !getUri().equals(field.getUri()) : field.getUri() != null) return false;
        if (getValue() != null ? !getValue().equals(field.getValue()) : field.getValue() != null) return false;
        if (getDefined_by() != null ? !getDefined_by().equals(field.getDefined_by()) : field.getDefined_by() != null)
            return false;
        return getDefinition() != null ? getDefinition().equals(field.getDefinition()) : field.getDefinition() == null;
    }

    @Override
    public int hashCode() {
        int result = getUri() != null ? getUri().hashCode() : 0;
        result = 31 * result + (getValue() != null ? getValue().hashCode() : 0);
        result = 31 * result + (getDefined_by() != null ? getDefined_by().hashCode() : 0);
        result = 31 * result + (getDefinition() != null ? getDefinition().hashCode() : 0);
        return result;
    }
}


