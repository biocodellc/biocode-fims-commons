package biocode.fims.config.models;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.LinkedList;

/**
 * A list of data to use in the validator.  We store data in lists because we find that different rules can refer
 * to the same list, and so we need only define them once.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class List {
    private String alias;
    private boolean caseInsensitive = false;
    private final LinkedList<Field> fields = new LinkedList<Field>();
    private boolean isNetworkList = false;

    @JsonCreator
    public List(String alias) {
        this.alias = alias;
    }

    /**
     * return the alias for which this list is known
     *
     * @return
     */
    public String getAlias() {
        return alias;
    }

    public boolean getCaseInsensitive() {
        return caseInsensitive;
    }

    public void setCaseInsensitive(boolean caseInsensitive) {
        this.caseInsensitive = caseInsensitive;
    }

    /**
     * Add a field that belongs to this list
     *
     * @param field
     */
    public void addField(Field field) {
        fields.add(field);
    }

    /**
     * Get the set of fields that belong to this list
     *
     * @return
     */
    public java.util.List<Field> getFields() {
        return fields;
    }

    public boolean isNetworkList() {
        return this.isNetworkList;
    }

    public void setNetworkList() {
        isNetworkList = true;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof List)) return false;

        List list = (List) o;

        if (getCaseInsensitive() != list.getCaseInsensitive()) return false;
        if (isNetworkList() != list.isNetworkList()) return false;
        if (getAlias() != null ? !getAlias().equals(list.getAlias()) : list.getAlias() != null) return false;
        return getFields() != null ? getFields().equals(list.getFields()) : list.getFields() == null;
    }

    @Override
    public int hashCode() {
        int result = getAlias() != null ? getAlias().hashCode() : 0;
        result = 31 * result + (getCaseInsensitive() ? 1 : 0);
        result = 31 * result + (getFields() != null ? getFields().hashCode() : 0);
        result = 31 * result + (isNetworkList() ? 1 : 0);
        return result;
    }
}
