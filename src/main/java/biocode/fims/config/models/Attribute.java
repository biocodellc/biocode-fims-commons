package biocode.fims.config.models;


import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.Arrays;

/**
 * Attribute representation
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Attribute implements Comparable {

    // set by network
    private String column;
    private String uri;
    private DataType dataType = DataType.STRING;  // string is default type
    private boolean internal = false;
    // allow projects to modify?
    private String definedBy;
    private String dataFormat;
    private String delimitedBy;

    // defaults
    private String group;
    private String definition;
    private boolean allowUnknown = false;
    private boolean allowTBD = false;

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
     * set the Column name.
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

    /**
     * Used w/ INTEGER, FLOAT, DATE, DATETIME, & TIME DataTypes
     * @return
     */
    // TODO move to DataType Object
    public boolean getAllowUnknown() {
        return allowUnknown;
    }

    public void setAllowUnknown(boolean allowUnknown) {
        this.allowUnknown = allowUnknown;
    }

    /**
     * Used w/ INTEGER, FLOAT, DATE, DATETIME, & TIME DataTypes
     * @return
     */
    // TODO move to DataType Object
    public boolean getAllowTBD() {
        return allowTBD;
    }

    public void setAllowTBD(boolean allowTBD) {
        this.allowTBD = allowTBD;
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

    public void setDefinition(String definition) {
        this.definition = definition;
    }

    // TODO move to DataType Object
    public String getDataFormat() {
        return dataFormat;
    }

    public void setDataFormat(String dataFormat) {
        this.dataFormat = dataFormat;
    }

    public boolean isInternal() {
        return internal;
    }

    public void setInternal(boolean internal) {
        this.internal = internal;
    }

    public int compareTo(Object o) {
        return 0;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public Attribute clone() {
        Attribute a = new Attribute(column, uri);

        a.dataType = dataType;
        a.dataFormat = dataFormat;
        a.internal = internal;
        a.definedBy = definedBy;
        a.delimitedBy = delimitedBy;

        a.group = group;
        a.definition = definition;
        a.allowUnknown = allowUnknown;

        return a;
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

    public static boolean isUnknownValue(String val) {
        return val != null && val.toLowerCase().equals("unknown");
    }

    public static boolean isTBDValue(String val) {
        return val != null && Arrays.asList("tbd", "to be determined").contains(val.toLowerCase());
    }
}
