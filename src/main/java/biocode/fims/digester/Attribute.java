package biocode.fims.digester;

import biocode.fims.settings.FimsPrinter;

import java.util.HashMap;
import java.util.Map;

/**
 * Attribute representation
 */
public class Attribute implements Comparable {
    private String isDefinedByURIString = "http://www.w3.org/2000/01/rdf-schema#isDefinedBy";

    private String group;
    private String column;
    private String column_internal;
    private String uri;
    private String defined_by;
    private DataType datatype = DataType.STRING;  // string is default type
    private String definition;
    private String synonyms;
    private String dataformat;
    private String delimited_by;
    private String type;
    private Boolean displayAnnotationProperty;

    public String getColumn() {
        return column;
    }

    /**
     * set the Column name. Here we normalize column names to replace spaces with underscore and remove an forward /'s
     * @param column
     */
    public void setColumn(String column) {
        // NOTE: do NOT use this syntax here, it will mess with application funcationality
        //this.column = column.replace(" ","_").replace("/","");
        // INSTEAD: use THIS syntax and handle replacements elsewhere.
        this.column = column;

    }

    /**
     * Default is true
     * @return
     */
    public Boolean getDisplayAnnotationProperty() {
        if (displayAnnotationProperty == null) {
            return true;
        }
        return displayAnnotationProperty;
    }

    public void setDisplayAnnotationProperty(Boolean displayAnnotationProperty) {
        this.displayAnnotationProperty = displayAnnotationProperty;
    }

    public String getIsDefinedByURIString() {
        return isDefinedByURIString;
    }

    public String getColumn_internal() {
        return column_internal;
    }

    public void setColumn_internal(String column_internal) {
        this.column_internal = column_internal;
    }

    public String getDelimited_by() {
        return delimited_by;
    }

    public void setDelimited_by(String delimited_by) {
        this.delimited_by = delimited_by;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
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

    public String getSynonyms() {
        return synonyms;
    }

    public void addSynonyms(String synonyms) {
        this.synonyms = synonyms;
    }

    public String getDataformat() {
        return dataformat;
    }

    public void setDataformat(String dataFormat) {
        this.dataformat = dataFormat;
    }

    /**
     * Basic Text printer
     */
    public void print() {
        FimsPrinter.out.println("  Attribute:");
        FimsPrinter.out.println("    column=" + column);
        FimsPrinter.out.println("    uri=" + uri);
        FimsPrinter.out.println("    datatype=" + datatype);
        FimsPrinter.out.println("    isDefinedBy=" + defined_by);
        FimsPrinter.out.println("    column_internal=" + column_internal);
    }

    public int compareTo(Object o) {
        return 0;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public Map getMap() {
        Map m = new HashMap();
        m.put("isDefinedByUriString", (isDefinedByURIString != null) ? isDefinedByURIString:"");
        m.put("group", (group != null) ? group:"");
        m.put("column", (column != null) ? column:"");
        m.put("columnInternal", (column_internal != null) ? column_internal:"");
        m.put("uri", (uri != null) ? uri:"");
        m.put("definedBy", (defined_by != null) ? defined_by:"");
        m.put("dataType", (datatype != null) ? datatype:"");
        m.put("definition", (definition != null) ? definition:"");
        m.put("synonyms", (synonyms != null) ? synonyms:"");
        m.put("dataFormat", (dataformat != null) ? dataformat:"");
        m.put("delimitedBy", (delimited_by != null) ? delimited_by:"");
        m.put("type", (type != null) ? type:"");

        return m;
    }
}
