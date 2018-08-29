package biocode.fims.config.models;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Hold contents of a Field that is part of a List
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Field {
    private String uri;
    private String value;

    // allow projects to modify?
    private String definedBy;
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

    public String getDefinedBy() {
        return definedBy;
    }

    public void setDefinedBy(String definedBy) {
        this.definedBy = definedBy;
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
        if (getDefinedBy() != null ? !getDefinedBy().equals(field.getDefinedBy()) : field.getDefinedBy() != null)
            return false;
        return getDefinition() != null ? getDefinition().equals(field.getDefinition()) : field.getDefinition() == null;
    }

    @Override
    public int hashCode() {
        int result = getUri() != null ? getUri().hashCode() : 0;
        result = 31 * result + (getValue() != null ? getValue().hashCode() : 0);
        result = 31 * result + (getDefinedBy() != null ? getDefinedBy().hashCode() : 0);
        result = 31 * result + (getDefinition() != null ? getDefinition().hashCode() : 0);
        return result;
    }
}


