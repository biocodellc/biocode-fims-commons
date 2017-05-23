package biocode.fims.elasticSearch.query;

import biocode.fims.digester.DataType;

/**
 * @author RJ Ewing
 */
public class ElasticSearchFilterField {

    private final String field;
    private final String displayName;
    private final DataType dataType;
    private final String group;
    private boolean nested = false;
    private String path;

    /**
     * @param field       how this ElasticSearch property is accessed
     * @param displayName how we should display this Filter to the user
     */
    public ElasticSearchFilterField(String field, String displayName, DataType dataType, String group) {
        this.field = field;
        this.displayName = displayName;
        this.dataType = dataType;
        this.group = group;
    }

    /**
     * if this field belongs to a nested object
     *
     * @param isNested
     * @return
     */
    public ElasticSearchFilterField nested(boolean isNested) {
        this.nested = isNested;
        return this;
    }

    /**
     * the path of the nested query
     * @param path
     * @return
     */
    public ElasticSearchFilterField path(String path) {
        this.path = path;
        return this;
    }

    /**
     * ElasticSearch property field path
     * @return
     */
    public String getField() {
        return field;
    }

    /**
     * Human readable name for this filter
     * @return
     */
    public String getDisplayName() {
        return displayName;
    }

    /**
     * is this a ElasticSearch nested filter
     * @return
     */
    public boolean isNested() {
        return nested;
    }

    /**
     * the ElasticSearch nested filter path
     * @return
     */
    public String getPath() {
        return path;
    }

    public DataType getDataType() {
        //TODO create an enum and use the ElasticSearch dataType
        return dataType;
    }

    public String getGroup() {
        return group;
    }

    public String exactMatchFieled() {
        if (DataType.STRING.equals(dataType)) {
            return field + ".keyword";
        }
        return field;
    }
}
