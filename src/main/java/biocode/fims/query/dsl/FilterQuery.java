package biocode.fims.query.dsl;

import java.util.ArrayList;
import java.util.List;

/**
 * @author rjewing
 */
public class FilterQuery implements QueryContainer {
    private List<QueryExpression> expressions;

    public FilterQuery() {
        expressions = new ArrayList<>();
    }

    @Override
    public void add(QueryExpression q) {
        expressions.add(q);
    }

    public List<QueryExpression> getExpressions() {
        return expressions;
    }
}
