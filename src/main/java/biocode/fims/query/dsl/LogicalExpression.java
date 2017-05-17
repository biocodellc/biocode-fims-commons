package biocode.fims.query.dsl;

import org.springframework.util.Assert;

/**
 * @author rjewing
 */
public class LogicalExpression implements Expression {
    private final Expression left;
    private final Expression right;
    private final LogicalOperator operator;

    public LogicalExpression(LogicalOperator operator, Expression left, Expression right) {
        Assert.notNull(operator);
        Assert.notNull(left);
        Assert.notNull(right);
        this.left = left;
        this.right = right;
        this.operator = operator;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof LogicalExpression)) return false;

        LogicalExpression that = (LogicalExpression) o;

        if (!left.equals(that.left)) return false;
        if (!right.equals(that.right)) return false;
        return operator == that.operator;
    }

    @Override
    public int hashCode() {
        int result = left.hashCode();
        result = 31 * result + right.hashCode();
        result = 31 * result + operator.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return "LogicalExpression{" +
                "left=" + left +
                ", operator=" + operator +
                ", right=" + right +
                '}';
    }
}
