package biocode.fims.query.dsl;

import biocode.fims.query.ExpressionVisitor;

/**
 * @author rjewing
 */
public class AllExpression implements Expression {
    @Override
    public void accept(ExpressionVisitor visitor) {
        visitor.visit(this);
    }

    @Override
    public boolean equals(Object o) {
        return this == o || o instanceof AllExpression;
    }
}
