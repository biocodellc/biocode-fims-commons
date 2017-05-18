package biocode.fims.query.dsl;

import biocode.fims.query.ExpressionVisitor;

/**
 * @author rjewing
 */
public class ComparisonExpression implements Expression {
    private final String column;
    private final String term;
    private final ComparisonOperator operator;

    public ComparisonExpression(String column, String term, ComparisonOperator operator) {
        this.column = column;
        this.term = term;
        this.operator = operator;
    }

    public String column() {
        return column;
    }

    public String term() {
        return term;
    }

    public ComparisonOperator operator() {
        return operator;
    }

    @Override
    public void accept(ExpressionVisitor visitor) {
        visitor.visit(this);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ComparisonExpression)) return false;

        ComparisonExpression that = (ComparisonExpression) o;

        if (!column.equals(that.column)) return false;
        return term.equals(that.term);
    }

    @Override
    public int hashCode() {
        int result = column.hashCode();
        result = 31 * result + term.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return column + operator + term;
    }
}
