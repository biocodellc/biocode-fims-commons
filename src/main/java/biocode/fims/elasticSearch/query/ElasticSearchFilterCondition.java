package biocode.fims.elasticSearch.query;

import biocode.fims.digester.DataType;

/**
 * @author RJ Ewing
 */
public class ElasticSearchFilterCondition {
    private final QueryOperator queryOperator = QueryOperator.EQUALS;
    private final ElasticSearchFilterField filterField;
    private final String value;
    private boolean regexp = false;

    public ElasticSearchFilterCondition(ElasticSearchFilterField filterField, String value) {
        this.filterField = filterField;
        this.value = value;
    }

    public ElasticSearchFilterCondition regexp(boolean isRegexp) {
        this.regexp = isRegexp;
        return this;
    }

    public boolean isRegexp() {
        return regexp;
    }

    public ElasticSearchFilterField getFilterField() {
        return filterField;
    }

    public String getValue() {
        return value;
    }

    public boolean isNested() {
        return filterField.isNested();
    }

    public String getPath() {
        return filterField.getPath();
    }

    public String getField() {
        if (DataType.STRING.equals(filterField.getDataType()) &&
                queryOperator.equals(QueryOperator.EQUALS)) {
            // to do an exact search in ElasticSearch we need to query the keyword index
            // TODO more formally set the .keyword filterField in ElasticSearchFilterField
            // currently assuming that all String dataType attributes contain a keyword filterField
            return filterField.getField() + ".keyword";
        }

        return filterField.getField();
    }
}
