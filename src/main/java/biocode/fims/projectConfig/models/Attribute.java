package biocode.fims.projectConfig.models;


import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Attribute representation
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Attribute implements Comparable {

    private String group;
    private String column;
    private String uri;
    private String definedBy;
    private DataType dataType = DataType.STRING;  // string is default type
    private String definition;
    private String dataFormat;
    private String delimitedBy;

    public Attribute() {
    }

    public Attribute(String column, String uri) {
        this.column = column;
        this.uri = uri;
    }

    public String getColumn() {
        return column;
    }

    /**
     * set the Column name. Here we normalize column names to replace spaces with underscore and remove an forward /'s
     *
     * @param column
     */
    public void setColumn(String column) {
        this.column = column;

    }

    public String getDelimitedBy() {
        return delimitedBy;
    }

    public void setDelimitedBy(String delimitedBy) {
        this.delimitedBy = delimitedBy;
    }

    public String getGroup() {
        return group;
    }

    public void setGroup(String group) {
        this.group = group;
    }

    public String getDefinedBy() {
        return definedBy;
    }

    public void setDefinedBy(String definedBy) {
        this.definedBy = definedBy;
    }

    // TODO move to DataType Object
    public DataType getDataType() {
        return dataType;
    }

    public void setDataType(DataType dataType) {
        this.dataType = dataType;
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
    public String getDataFormat() {
        return dataFormat;
    }

    public void setDataFormat(String dataFormat) {
        this.dataFormat = dataFormat;
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
