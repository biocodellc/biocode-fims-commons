package biocode.fims.query;

import java.io.Serializable;

/**
 * @author rjewing
 */
public class QueryCriteria implements Serializable {

    private String key;
    private String value;
    private QueryType type;

    private QueryCriteria() {}

    public QueryCriteria(String key, String value, QueryType type) {
        this.key = key;
        this.value = value;
        this.type = type;
    }

    public String getKey() {
        return key;
    }

    public String getValue() {
        return value;
    }

    public QueryType getType() {
        return type;
    }
}
