package biocode.fims.elasticSearch;

import biocode.fims.elasticSearch.query.ElasticSearchFilterField;
import biocode.fims.fimsExceptions.FimsRuntimeException;
import biocode.fims.fimsExceptions.errorCodes.QueryCode;

import java.util.List;

/**
 * @author rjewing
 */
public class FieldColumnTransformer {

    private final List<ElasticSearchFilterField> filterFields;

    public FieldColumnTransformer(List<ElasticSearchFilterField> filterFields) {

        this.filterFields = filterFields;
    }

    public ElasticSearchFilterField getFilterField(String column) {
        for (ElasticSearchFilterField filter : filterFields) {
            if (column.equals(filter.getDisplayName())) {
                return filter;
            }
        }

        throw new FimsRuntimeException(QueryCode.UNKNOWN_FILTER, "is " + column + " a filterable field?", 400, column);
    }
}
