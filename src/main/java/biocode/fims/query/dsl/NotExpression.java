package biocode.fims.query.dsl;

import biocode.fims.query.ExpressionVisitor;
import org.springframework.util.Assert;

/**
 * @author rjewing
 */
public class NotExpression implements Expression {
    private final Expression expression;

    public NotExpression(Expression expression) {
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
        if (!(o instanceof NotExpression)) return false;

        NotExpression that = (NotExpression) o;

        return expression.equals(that.expression);
    }

    @Override
    public int hashCode() {
        return expression.hashCode();
    }

    @Override
    public String toString() {
        return "NotExpression{" +
                expression +
                '}';
    }
}
