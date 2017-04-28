package biocode.fims.digester;

import java.util.LinkedList;

/**
 * A list of data to use in the validator.  We store data in lists because we find that different rules can refer
 * to the same list, and so we need only define them once.
 */
public class List {
    private String alias;
    private String caseInsensitive;
    //private java.util.List fields = new ArrayList();
     // Loop all the lists associated with the validation element
    private final LinkedList<Field> fields = new LinkedList<Field>();

    /**
     * return the alias for which this list is known
     *
     * @return
     */
    public String getAlias() {
        return alias;
    }

    /**
     * set the alias by which this list is known
     *
     * @param alias
     */
    public void setAlias(String alias) {
        this.alias = alias;
    }

    public String getCaseInsensitive() {
        return caseInsensitive;
    }

    public void setCaseInsensitive(String caseInsensitive) {
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

    public void print() {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public void run(Object o) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof List)) return false;

        List list = (List) o;

        if (getAlias() != null ? !getAlias().equals(list.getAlias()) : list.getAlias() != null) return false;
        if (getCaseInsensitive() != null ? !getCaseInsensitive().equals(list.getCaseInsensitive()) : list.getCaseInsensitive() != null)
            return false;
        return getFields().equals(list.getFields());
    }

    @Override
    public int hashCode() {
        int result = getAlias() != null ? getAlias().hashCode() : 0;
        result = 31 * result + (getCaseInsensitive() != null ? getCaseInsensitive().hashCode() : 0);
        result = 31 * result + getFields().hashCode();
        return result;
    }
}
