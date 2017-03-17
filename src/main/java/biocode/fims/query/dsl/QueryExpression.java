package biocode.fims.query.dsl;

import org.elasticsearch.index.query.QueryBuilder;

import java.util.List;

/**
 * @author rjewing
 */
public interface QueryExpression {

    List<QueryBuilder> getQueryBuilders();

}
