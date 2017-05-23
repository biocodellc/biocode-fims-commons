package biocode.fims.elasticSearch.query;

import biocode.fims.fimsExceptions.FimsRuntimeException;
import biocode.fims.fimsExceptions.errorCodes.QueryCode;
import biocode.fims.query.QueryType;
import org.apache.lucene.search.join.ScoreMode;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;

/**
 * @author RJ Ewing
 */
public class ElasticSearchFilterCondition {
    private final ElasticSearchFilterField filterField;
    private final String value;
    private final QueryType queryType;

    public ElasticSearchFilterCondition(ElasticSearchFilterField filterField, String value, QueryType queryType) {
        this.filterField = filterField;
        this.value = value;
        this.queryType = queryType;
    }

    public QueryBuilder getQuery() {
        QueryBuilder query;

        switch(queryType) {
            case FUZZY:
                query = getFuzzyQuery();
                break;
            case EXISTS:
                query = getExistsQuery();
                break;
            case GREATER_THEN:
                query = getGTQuery();
                break;
            case LESS_THEN:
                query = getLTQuery();
                break;
            case GREATER_THEN_EQUALS:
                query = getGTEQuery();
                break;
            case LESS_THEN_EQUALS:
                query = getLTEQuery();
                break;
            case EQUALS:
            default:
                query = getEqualsQuery();
        }

        if (filterField.isNested()) {
            return nestedQuery(query);
        }
        return query;
    }

    private QueryBuilder getFuzzyQuery() {
        checkValidQueryType(QueryType.FUZZY);
        return QueryBuilders.matchQuery(filterField.getField(), value);
    }

    private QueryBuilder getExistsQuery() {
        checkValidQueryType(QueryType.EXISTS);
        return QueryBuilders.existsQuery(filterField.getField());
    }

    private QueryBuilder getGTQuery() {
        checkValidQueryType(QueryType.GREATER_THEN);
        return QueryBuilders.rangeQuery(filterField.getField())
                .gt(value);
    }

    private QueryBuilder getLTQuery() {
        checkValidQueryType(QueryType.LESS_THEN);
        return QueryBuilders.rangeQuery(filterField.getField())
                .lt(value);
    }

    private QueryBuilder getGTEQuery() {
        checkValidQueryType(QueryType.GREATER_THEN_EQUALS);
        return QueryBuilders.rangeQuery(filterField.getField())
                .gte(value);
    }

    private QueryBuilder getLTEQuery() {
        checkValidQueryType(QueryType.LESS_THEN_EQUALS);
        return QueryBuilders.rangeQuery(filterField.getField())
                .lte(value);
    }

    private QueryBuilder getEqualsQuery() {
        checkValidQueryType(QueryType.EQUALS);
        return QueryBuilders.matchQuery(filterField.exactMatchFieled(), value);
    }


    private void checkValidQueryType(QueryType type) {
        if (!type.getDataTypes().contains(filterField.getDataType())) {
            throw new FimsRuntimeException(QueryCode.INVALID_TYPE, 400, type.name(), filterField.getDisplayName());
        }
    }

    private QueryBuilder nestedQuery(QueryBuilder query) {
        return QueryBuilders.nestedQuery(
                filterField.getPath(),
                query,
                ScoreMode.None
        );
    }
}
