package biocode.fims.query.dsl;


/**
 * @author rjewing
 */
public class Query {

    private final Expression expression;

    public Query(Expression expression) {
       this.expression = expression;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Query)) return false;

        Query query = (Query) o;

        return expression != null ? expression.equals(query.expression) : query.expression == null;
    }

    @Override
    public int hashCode() {
        return expression != null ? expression.hashCode() : 0;
    }

    @Override
    public String toString() {
        return expression.toString();
    }
}
