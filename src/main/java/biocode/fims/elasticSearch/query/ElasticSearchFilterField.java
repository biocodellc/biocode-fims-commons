package biocode.fims.elasticSearch.query;

/**
 * @author RJ Ewing
 */
public class ElasticSearchFilterField {

    private final String field;
    private final String displayName;
    private boolean nested = false;
    private String path;

    /**
     * @param field       how this ElasticSearch property is accessed
     * @param displayName how we should display this Filter to the user
     */
    public ElasticSearchFilterField(String field, String displayName) {
        this.field = field;
        this.displayName = displayName;
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
}
