package biocode.fims.query.dsl;

import biocode.fims.elasticSearch.FieldColumnTransformer;
import org.elasticsearch.index.query.QueryBuilder;

import java.util.Collection;
import java.util.List;

/**
 * @author rjewing
 */
public interface QueryExpression {

    List<QueryBuilder> getQueryBuilders();

    void setColumn(FieldColumnTransformer transformer, String column);

    List<QueryExpression> getExpressions(String column);
}
