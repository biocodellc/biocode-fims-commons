package biocode.fims.query.dsl;

/**
 * @author rjewing
 */
public class EmptyExpression implements Expression {
    @Override
    public boolean equals(Object o) {
        return this == o || o instanceof EmptyExpression;
    }
}
