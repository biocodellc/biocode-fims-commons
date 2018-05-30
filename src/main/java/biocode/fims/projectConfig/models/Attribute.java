package biocode.fims.projectConfig.models;


/**
 * Attribute representation
 */
public class Attribute implements Comparable {

    // TODO make fields camelCase
    private String group;
    private String column;
    private String uri;
    private String defined_by;
    private DataType datatype = DataType.STRING;  // string is default type
    private String definition;
    private String dataformat;
    private String delimited_by;

    public Attribute() {}

    public Attribute(String column, String uri) {
        this.column = column;
        this.uri = uri;
    }

    public String getColumn() {
        return column;
    }

    /**
     * set the Column name. Here we normalize column names to replace spaces with underscore and remove an forward /'s
     * @param column
     */
    public void setColumn(String column) {
        this.column = column;

    }

    public String getDelimited_by() {
        return delimited_by;
    }

    public void setDelimited_by(String delimited_by) {
        this.delimited_by = delimited_by;
    }

    public String getGroup() {
        return group;
    }

    public void setGroup(String group) {
        this.group = group;
    }

    public String getDefined_by() {
        return defined_by;
    }

    public void setDefined_by(String defined_by) {
        this.defined_by = defined_by;
    }

    // TODO move to DataType Object
    public DataType getDatatype() {
        return datatype;
    }

    public void setDatatype(DataType datatype) {
        this.datatype = datatype;
    }

    public String getUri() {
        return uri;
    }

    public void setUri(String uri) {
        this.uri = uri;
    }

    public String getDefinition() {
        return definition;
    }

    public void addDefinition(String definition) {
        this.definition = definition;
    }

    // TODO move to DataType Object
    public String getDataformat() {
        return dataformat;
    }

    public void setDataformat(String dataFormat) {
        this.dataformat = dataFormat;
    }


    public int compareTo(Object o) {
        return 0;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Attribute)) return false;

        Attribute attribute = (Attribute) o;

        return getUri() != null ? getUri().equals(attribute.getUri()) : attribute.getUri() == null;
    }

    @Override
    public int hashCode() {
        return getUri() != null ? getUri().hashCode() : 0;
    }
}
