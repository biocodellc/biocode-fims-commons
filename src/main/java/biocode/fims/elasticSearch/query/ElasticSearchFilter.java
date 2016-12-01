package biocode.fims.elasticSearch.query;

/**
 * @author RJ Ewing
 */
public class ElasticSearchFilter {

    private final String field;
    private final String displayName;
    private boolean nested = false;
    private String path;

    /**
     * @param field       how this ElasticSearch property is accessed
     * @param displayName how we should display this Filter to the user
     */
    public ElasticSearchFilter(String field, String displayName) {
        this.field = field;
        this.displayName = displayName;
    }

    /**
     * if this field belongs to a nested object
     *
     * @param isNested
     * @return
     */
    public ElasticSearchFilter nested(boolean isNested) {
        this.nested = isNested;
        return this;
    }

    /**
     * the path of the nested query
     * @param path
     * @return
     */
    public ElasticSearchFilter path(String path) {
        this.path = path;
        return this;
    }

    public String getField() {
        return field;
    }

    public String getDisplayName() {
        return displayName;
    }

    public boolean isNested() {
        return nested;
    }

    public String getPath() {
        return path;
    }
}
