package biocode.fims.query.dsl;

import biocode.fims.query.ExpressionVisitor;
import org.springframework.util.Assert;

/**
 * @author rjewing
 */
public class GroupExpression implements Expression {
    private final Expression expression;

    public GroupExpression(Expression expression) {
        Assert.notNull(expression);
        this.expression = expression;
    }

    public Expression expression() {
        return expression;
    }

    @Override
    public void accept(ExpressionVisitor visitor) {
        visitor.visit(this);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof GroupExpression)) return false;

        GroupExpression that = (GroupExpression) o;

        return expression.equals(that.expression);
    }

    @Override
    public int hashCode() {
        return expression.hashCode();
    }

    @Override
    public String toString() {
        return "GroupExpression{" +
                expression +
                '}';
    }
}
