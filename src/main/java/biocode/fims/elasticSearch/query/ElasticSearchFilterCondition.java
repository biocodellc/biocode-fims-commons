package biocode.fims.elasticSearch.query;

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
            case GREATER_THEN_EQUAL:
                query = getGTEQuery();
                break;
            case LESS_THEN_EQAUL:
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
        return QueryBuilders.matchQuery(filterField.getField(), value);
    }

    private QueryBuilder getExistsQuery() {
        return QueryBuilders.existsQuery(filterField.getField());
    }

    private QueryBuilder getGTQuery() {
        return QueryBuilders.rangeQuery(filterField.getField())
                .gt(value);
    }

    private QueryBuilder getLTQuery() {
        return QueryBuilders.rangeQuery(filterField.getField())
                .lt(value);
    }

    private QueryBuilder getGTEQuery() {
        return QueryBuilders.rangeQuery(filterField.getField())
                .gte(value);
    }

    private QueryBuilder getLTEQuery() {
        return QueryBuilders.rangeQuery(filterField.getField())
                .lte(value);
    }

    private QueryBuilder getEqualsQuery() {
        return QueryBuilders.matchQuery(filterField.exactMatchFieled(), value);
    }

    private QueryBuilder nestedQuery(QueryBuilder query) {
        return QueryBuilders.nestedQuery(
                filterField.getPath(),
                query,
                ScoreMode.None
        );
    }
}
